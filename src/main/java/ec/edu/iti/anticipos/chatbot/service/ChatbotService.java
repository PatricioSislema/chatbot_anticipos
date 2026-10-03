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

    // Tiempo máximo de inactividad
    private static final long TIMEOUT_MINUTOS = 30;

    // ==================== MENSAJES ====================
    public String procesarMensaje(String mensaje, Long chatId) {
        System.out.println("=== PROCESAR MENSAJE ===");
        System.out.println("ChatId: [" + chatId + "]");
        System.out.println("Mensaje: [" + mensaje + "]");

        String mensajeLower = mensaje.toLowerCase().trim();

        SesionUsuario sesion = sesiones.get(chatId);
        if (sesion == null) {
            System.out.println("Sesion NO existe. Creando nueva...");
            sesion = new SesionUsuario();
            sesiones.put(chatId, sesion);
        } else {
            System.out.println("Sesion existe. Autenticado: " + sesion.isAutenticado()
                    + " | EsperandoContrasena: " + sesion.isEsperandoContrasena()
                    + " | Correo: " + sesion.getCorreo());
        }

        // Verificar expiración de sesión
        if (sesion.isAutenticado() && expiroSesion(sesion)) {
            sesiones.remove(chatId);
            return "Tu sesion ha expirado por inactividad (5 minutos). Por favor, ingresa nuevamente tu correo y contrasena.";
        }

        sesion.setUltimaActividad(LocalDateTime.now());

        // COMANDO PARA CERRAR SESIÓN
        if (mensajeLower.equals("salir") || mensajeLower.equals("cerrar sesion")
                || mensajeLower.equals("cerrar sesión") || mensajeLower.equals("logout")
                || mensajeLower.equals("5")) {

            if (mensajeLower.equals("5") && !sesion.isAutenticado()) {
                // Si no está autenticado, tratar como mensaje normal
            } else {
                sesiones.remove(chatId);
                return "Sesion cerrada correctamente. Hasta luego.\n\n" +
                        "Para volver a ingresar, escribe *hola*.";
            }
        }

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
            return "Debes iniciar sesion primero.";
        }

        if (sesion.getRolNombre().equalsIgnoreCase("Docente")) {
            String respuesta = docenteChatService.procesarDocumento(archivo, sesion);

            if (respuesta != null && (respuesta.contains("creada con éxito")
                    || respuesta.contains("registrada con éxito")
                    || respuesta.contains("Devolución registrada")
                    || respuesta.contains("cerrada"))) {
                return respuesta + "\n\n" + menuDespuesDeTramite(sesion);
            }

            return respuesta;
        }

        return "Tu rol no permite subir documentos.";
    }

    // ==================== DELEGACIÓN POR ROL ====================
    private String delegarPorRol(String mensaje, String mensajeLower, SesionUsuario sesion) {
        String rol = sesion.getRolNombre();

        // ============================================
        // VALIDACIÓN: Si espera PDF y el usuario envía texto
        // ============================================
        String accion = sesion.getAccionPendiente();
        if (accion != null && !accion.isEmpty()) {
            if (accion.equals("SOLICITAR") || accion.equals("LIQUIDAR")
                    || accion.equals("FACTURAS") || accion.equals("DEVOLUCION")) {

                boolean esConfirmacion = mensajeLower.equals("si") || mensajeLower.equals("sí")
                        || mensajeLower.equals("no") || mensajeLower.equals("confirmo")
                        || mensajeLower.equals("listo") || mensajeLower.equals("1")
                        || mensajeLower.equals("2") || mensajeLower.equals("3")
                        || mensajeLower.equals("4") || mensajeLower.equals("5")
                        || mensajeLower.contains("ayuda")
                        || mensajeLower.contains("opciones") || mensajeLower.contains("menu")
                        || mensajeLower.contains("menú") || mensajeLower.contains("salir");

                if (esConfirmacion) {
                    // No bloquear
                } else {
                    boolean esTexto = !mensajeLower.contains(".pdf")
                            && !mensajeLower.startsWith("http");

                    if (esTexto) {
                        switch (accion) {
                            case "SOLICITAR":
                                return "Archivo incorrecto. Por favor, sube el *formulario de solicitud de anticipo* en formato PDF.";
                            case "LIQUIDAR":
                                return "Archivo incorrecto. Por favor, sube el *formulario de liquidacion* en formato PDF.";
                            case "FACTURAS":
                                return "Archivo incorrecto. Por favor, sube las *facturas* en formato PDF.";
                            case "DEVOLUCION":
                                return "Archivo incorrecto. Por favor, sube el *comprobante de devolucion* en formato PDF.";
                        }
                    }
                }
            }
        }

        // ============================================
        // DELEGACIÓN POR ROL
        // ============================================

        // Docente
        if (rol.equalsIgnoreCase("Docente")) {
            String respuesta = docenteChatService.procesar(mensaje, mensajeLower, sesion);
            if (respuesta != null) {
                if (respuesta.contains("creada con éxito")
                        || respuesta.contains("registrada con éxito")
                        || respuesta.contains("Devolución registrada")
                        || respuesta.contains("cerrada")) {
                    return respuesta + "\n\n" + menuDespuesDeTramite(sesion);
                }
                return respuesta;
            }
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
            if (respuesta != null) {
                if (respuesta.contains("aprobada") || respuesta.contains("rechazada")) {
                    return respuesta + "\n\n" + menuDespuesDeTramite(sesion);
                }
                return respuesta;
            }
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
            if (respuesta != null) {
                if (respuesta.contains("aprobada") || respuesta.contains("rechazada")) {
                    return respuesta + "\n\n" + menuDespuesDeTramite(sesion);
                }
                return respuesta;
            }
        }

        // Contadora
        if (rol.equalsIgnoreCase("Contadora")) {
            String respuesta = contadoraChatService.procesar(mensaje, mensajeLower, sesion);
            if (respuesta != null) {
                if (respuesta.contains("aprobado") || respuesta.contains("rechazado")
                        || respuesta.contains("aprobada") || respuesta.contains("rechazada")) {
                    return respuesta + "\n\n" + menuDespuesDeTramite(sesion);
                }
                return respuesta;
            }
        }

        // Colectora
        if (rol.equalsIgnoreCase("Colectora")) {
            String respuesta = colectoraChatService.procesar(mensaje, mensajeLower, sesion);
            if (respuesta != null) {
                if (respuesta.contains("registrada") || respuesta.contains("Transferencia")) {
                    return respuesta + "\n\n" + menuDespuesDeTramite(sesion);
                }
                return respuesta;
            }
        }

        // Asistente Contable
        if (rol.equalsIgnoreCase("Asistente Contable")) {
            String respuesta = asistenteContableChatService.procesar(mensaje, mensajeLower, sesion);
            if (respuesta != null) {
                if (respuesta.contains("enviada") || respuesta.contains("derivada")) {
                    return respuesta + "\n\n" + menuDespuesDeTramite(sesion);
                }
                return respuesta;
            }
        }

        // Administrador
        if (rol.equalsIgnoreCase("Administrador")) {
            if (mensajeLower.contains("usuario")) {
                return "Mostrando gestion de usuarios...\n\n" + menuDespuesDeTramite(sesion);
            }
            if (mensajeLower.contains("historial")) {
                return "Mostrando historial general...\n\n" + menuDespuesDeTramite(sesion);
            }
        }

        // Si nada coincide, usar IA
        return openAIService.interpretarMensaje(mensaje);
    }

    // ==================== MENÚ DESPUÉS DE TRÁMITE ====================
    private String menuDespuesDeTramite(SesionUsuario sesion) {
        String rol = sesion.getRolNombre();

        StringBuilder menu = new StringBuilder();
        menu.append("Deseas hacer otro tramite?\n\n");

        if (rol.equalsIgnoreCase("Docente")) {
            menu.append("1. Solicitar un anticipo\n");
            menu.append("2. Liquidar un anticipo\n");
            menu.append("3. Consultar el estado de tus tramites\n");
            menu.append("4. Resolver dudas generales\n");
            menu.append("5. Salir\n\n");
        } else if (rol.equalsIgnoreCase("Jefe Inmediato")) {
            menu.append("1. Revisar solicitudes pendientes\n");
            menu.append("2. Aprobar o rechazar solicitudes\n");
            menu.append("3. Consultar historial\n");
            menu.append("4. Resolver dudas generales\n");
            menu.append("5. Salir\n\n");
        } else if (rol.equalsIgnoreCase("Director Financiero")) {
            menu.append("1. Revisar solicitudes pendientes\n");
            menu.append("2. Aprobar o rechazar solicitudes\n");
            menu.append("3. Consultar historial\n");
            menu.append("4. Resolver dudas generales\n");
            menu.append("5. Salir\n\n");
        } else if (rol.equalsIgnoreCase("Contadora")) {
            menu.append("1. Revisar solicitudes y liquidaciones\n");
            menu.append("2. Aprobar o rechazar tramites\n");
            menu.append("3. Consultar historial\n");
            menu.append("4. Resolver dudas generales\n");
            menu.append("5. Salir\n\n");
        } else if (rol.equalsIgnoreCase("Colectora")) {
            menu.append("1. Ver transferencias pendientes\n");
            menu.append("2. Registrar transferencias\n");
            menu.append("3. Consultar historial\n");
            menu.append("4. Resolver dudas generales\n");
            menu.append("5. Salir\n\n");
        } else if (rol.equalsIgnoreCase("Asistente Contable")) {
            menu.append("1. Revisar liquidaciones pendientes\n");
            menu.append("2. Enviar liquidaciones a la Contadora\n");
            menu.append("3. Consultar historial\n");
            menu.append("4. Resolver dudas generales\n");
            menu.append("5. Salir\n\n");
        } else {
            menu.append("1. Continuar\n");
            menu.append("2. Resolver dudas generales\n");
            menu.append("5. Salir\n\n");
        }

        menu.append("Escribe el numero de la opcion o *salir* para cerrar sesion.");
        return menu.toString();
    }

    // ==================== LOGIN ====================
    private String manejarLogin(String mensaje, String mensajeLower, SesionUsuario sesion) {

        // Si NO está esperando contraseña, procesar el correo
        if (!sesion.isEsperandoContrasena()) {

            // Validar formato de correo
            String correo = mensaje.trim();
            if (!correo.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
                return "Hola. Soy ITI, el asistente virtual del Instituto Tecnologico Internacional. " +
                        "Para continuar, ingresa tu correo electronico.";
            }

            // Validar que el correo exista en la base de datos
            if (!usuarioService.existeCorreo(correo)) {
                return "El correo " + correo + " no esta registrado en el sistema. Verifica e intenta de nuevo.";
            }

            sesion.setCorreo(correo);
            sesion.setEsperandoContrasena(true);
            return "Correo verificado. Ahora ingresa tu contrasena.";
        }

        // Si YA está esperando contraseña, procesar el login
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

            String bienvenida = "Hola " + response.getNombre() + ". Soy ITI, tu asistente virtual.\n\n";
            String rol = response.getRolNombre();

            if (rol.equalsIgnoreCase("Docente")) {
                bienvenida += "Como Docente, puedo ayudarte con:\n" +
                        "1. Solicitar un anticipo\n" +
                        "2. Liquidar un anticipo\n" +
                        "3. Consultar el estado de tus tramites\n" +
                        "4. Resolver dudas generales\n" +
                        "5. Salir\n\n" +
                        "Que necesitas hacer hoy?";
            } else if (rol.equalsIgnoreCase("Jefe Inmediato")) {
                bienvenida += "Como Jefe Inmediato, puedo ayudarte con:\n" +
                        "1. Revisar solicitudes pendientes\n" +
                        "2. Aprobar o rechazar solicitudes\n" +
                        "3. Consultar historial\n" +
                        "4. Resolver dudas generales\n" +
                        "5. Salir\n\n" +
                        "Que necesitas hacer hoy?";
            } else if (rol.equalsIgnoreCase("Director Financiero")) {
                bienvenida += "Como Director Financiero, puedo ayudarte con:\n" +
                        "1. Revisar solicitudes pendientes de autorizacion\n" +
                        "2. Aprobar o rechazar solicitudes\n" +
                        "3. Consultar historial\n" +
                        "4. Resolver dudas generales\n" +
                        "5. Salir\n\n" +
                        "Que necesitas hacer hoy?";
            } else if (rol.equalsIgnoreCase("Contadora")) {
                bienvenida += "Como Contadora, puedo ayudarte con:\n" +
                        "1. Revisar solicitudes y liquidaciones\n" +
                        "2. Aprobar o rechazar tramites\n" +
                        "3. Consultar historial\n" +
                        "4. Resolver dudas generales\n" +
                        "5. Salir\n\n" +
                        "Que necesitas hacer hoy?";
            } else if (rol.equalsIgnoreCase("Colectora")) {
                bienvenida += "Como Colectora, puedo ayudarte con:\n" +
                        "1. Ver transferencias pendientes\n" +
                        "2. Registrar transferencias\n" +
                        "3. Consultar historial\n" +
                        "4. Resolver dudas generales\n" +
                        "5. Salir\n\n" +
                        "Que necesitas hacer hoy?";
            } else if (rol.equalsIgnoreCase("Asistente Contable")) {
                bienvenida += "Como Asistente Contable, puedo ayudarte con:\n" +
                        "1. Revisar liquidaciones pendientes\n" +
                        "2. Enviar liquidaciones a la Contadora\n" +
                        "3. Consultar historial\n" +
                        "4. Resolver dudas generales\n" +
                        "5. Salir\n\n" +
                        "Que necesitas hacer hoy?";
            } else {
                bienvenida += "Que necesitas hacer hoy?\n\n" +
                        "5. Salir";
            }

            return bienvenida;

        } else {
            sesion.setEsperandoContrasena(false);
            sesion.setCorreo(null);
            return "Credenciales incorrectas. Por favor, ingresa tu correo electronico nuevamente.";
        }
    }

    // ==================== UTILIDADES ====================
    private boolean expiroSesion(SesionUsuario sesion) {
        return Duration.between(sesion.getUltimaActividad(), LocalDateTime.now())
                .toMinutes() > TIMEOUT_MINUTOS;
    }
}