package ec.edu.iti.anticipos.chatbot.service;

import ec.edu.iti.anticipos.chatbot.dto.request.LoginRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.LoginResponseDTO;
import ec.edu.iti.anticipos.chatbot.service.chat.*;
import ec.edu.iti.anticipos.chatbot.sesion.SesionUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio coordinador del chatbot.
 * Maneja la autenticación y delega la lógica a los servicios especializados por rol.
 */
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final OpenAIService openAIService;
    private final UsuarioService usuarioService;
    private final DocenteChatService docenteChatService;
    private final AprobadorChatService aprobadorChatService;
    private final ColectoraChatService colectoraChatService;
    private final AsistenteContableChatService asistenteContableChatService;
    private final ContadoraChatService contadoraChatService;


    // Mapa de sesiones activas por chatId
    private final Map<Long, SesionUsuario> sesiones = new HashMap<>();

    // Tiempo máximo de inactividad (30 minutos)
    private static final long TIMEOUT_MINUTOS = 30;

    // ==================== MENSAJES ====================
    public String procesarMensaje(String mensaje, Long chatId) {

        String mensajeLower = mensaje.toLowerCase().trim();

        SesionUsuario sesion = sesiones.get(chatId);
        if (sesion == null) {
            sesion = new SesionUsuario();
            sesiones.put(chatId, sesion);
        }

        // Verificar expiración de sesión
        if (sesion.isAutenticado() && expiroSesion(sesion)) {
            sesiones.remove(chatId);
            return "Tu sesión ha expirado por inactividad. Por favor, ingresa nuevamente tu correo y contraseña.";
        }

        sesion.setUltimaActividad(LocalDateTime.now());

        // Login si no está autenticado
        if (!sesion.isAutenticado()) {
            return manejarLogin(mensaje, mensajeLower, sesion);
        }

        // Delegar según rol
        return delegarPorRol(mensaje, mensajeLower, sesion);
    }

    // ==================== DOCUMENTOS ====================
    public String procesarDocumento(MultipartFile archivo, Long chatId) {

        SesionUsuario sesion = sesiones.get(chatId);

        if (sesion == null || !sesion.isAutenticado()) {
            return "Debes iniciar sesión primero.";
        }

        // Solo el docente sube documentos
        if (sesion.getRolNombre().equalsIgnoreCase("Docente")) {
            return docenteChatService.procesarDocumento(archivo, sesion);
        }

        return "Tu rol no permite subir documentos.";
    }

    // ==================== DELEGACIÓN POR ROL ====================
    private String delegarPorRol(String mensaje, String mensajeLower, SesionUsuario sesion) {
        String rol = sesion.getRolNombre();

        // Docente
        if (rol.equalsIgnoreCase("Docente")) {
            String respuesta = docenteChatService.procesar(mensaje, mensajeLower, sesion);
            if (respuesta != null) return respuesta;
        }

        // Jefe Inmediato
        if (rol.equalsIgnoreCase("Jefe Inmediato")) {
            ConfiguracionAprobador config = new ConfiguracionAprobador(
                    "Pendiente",
                    "Aprobado por Jefe",
                    "carlos.ruiz@iti.edu.ec",
                    "Jefe",
                    "juan.perez@iti.edu.ec"
            );
            String respuesta = aprobadorChatService.procesar(mensaje, mensajeLower, sesion, config);
            if (respuesta != null) return respuesta;
        }

        // Director Financiero
        if (rol.equalsIgnoreCase("Director Financiero")) {
            ConfiguracionAprobador config = new ConfiguracionAprobador(
                    "Aprobado por Jefe",
                    "Aprobado por Director",
                    "ana.torres@iti.edu.ec",
                    "Director",
                    "juan.perez@iti.edu.ec"
            );
            String respuesta = aprobadorChatService.procesar(mensaje, mensajeLower, sesion, config);
            if (respuesta != null) return respuesta;
        }

        // Contadora
        if (rol.equalsIgnoreCase("Contadora")) {
            String respuesta = contadoraChatService.procesar(mensaje, mensajeLower, sesion);
            if (respuesta != null) return respuesta;
        }

        // Colectora
        if (rol.equalsIgnoreCase("Colectora")) {
            String respuesta = colectoraChatService.procesar(mensaje, mensajeLower, sesion);
            if (respuesta != null) return respuesta;
        }

        // Asistente Contable
        if (rol.equalsIgnoreCase("Asistente Contable")) {
            String respuesta = asistenteContableChatService.procesar(mensaje, mensajeLower, sesion);
            if (respuesta != null) return respuesta;
        }

        // Administrador
        if (rol.equalsIgnoreCase("Administrador")) {
            if (mensajeLower.contains("usuario")) {
                return "Mostrando gestión de usuarios...";
            }
            if (mensajeLower.contains("historial")) {
                return "Mostrando historial general...";
            }
        }

        // Si nada coincide, usar IA
        return openAIService.interpretarMensaje(mensaje);
    }

    // ==================== LOGIN ====================
    private String manejarLogin(String mensaje, String mensajeLower, SesionUsuario sesion) {

        if (!sesion.isEsperandoContrasena()) {
            if (mensajeLower.contains("hola") || mensajeLower.contains("buenos")
                    || mensajeLower.contains("buenas")) {
                return "¡Hola! Soy ITI, el asistente virtual del Instituto Tecnológico Internacional. " +
                        "Para continuar, ingresa tu correo electrónico.";
            }
            sesion.setCorreo(mensaje.trim());
            sesion.setEsperandoContrasena(true);
            return "Ahora ingresa tu contraseña.";
        }

        LoginRequestDTO loginDTO = new LoginRequestDTO();
        loginDTO.setCorreo(sesion.getCorreo());
        loginDTO.setContrasena(mensaje.trim());

        LoginResponseDTO response = usuarioService.login(loginDTO);

        if (response.isAutenticado()) {
            sesion.setUsuarioId(response.getUsuarioId());
            sesion.setNombre(response.getNombre());
            sesion.setRolNombre(response.getRolNombre());
            sesion.setAutenticado(true);
            sesion.setEsperandoContrasena(false);
            return "Bienvenido " + response.getNombre() + " (" + response.getRolNombre() + "). " +
                    "¿Qué necesitas hacer hoy?";
        } else {
            sesion.setEsperandoContrasena(false);
            sesion.setCorreo(null);
            return "Credenciales incorrectas. Por favor, ingresa tu correo electrónico nuevamente.";
        }
    }

    // ==================== UTILIDADES ====================
    private boolean expiroSesion(SesionUsuario sesion) {
        return Duration.between(sesion.getUltimaActividad(), LocalDateTime.now())
                .toMinutes() > TIMEOUT_MINUTOS;
    }
}