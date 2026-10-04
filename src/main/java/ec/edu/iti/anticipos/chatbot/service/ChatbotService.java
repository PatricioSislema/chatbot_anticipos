package ec.edu.iti.anticipos.chatbot.service;

import ec.edu.iti.anticipos.chatbot.dto.request.LoginRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.LoginResponseDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.ResponseBot;
import ec.edu.iti.anticipos.chatbot.service.chat.*;
import ec.edu.iti.anticipos.chatbot.sesion.SesionUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ec.edu.iti.anticipos.chatbot.dto.response.DevolucionResponseDTO;
import ec.edu.iti.anticipos.chatbot.service.DevolucionService;
import java.util.Optional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio coordinador del chatbot.
 * Maneja la autenticación, los comandos generales, la clasificación de
 * intenciones con IA, la ayuda contextual, la cancelación y el cierre
 * de sesión. Delega la lógica específica a los servicios por rol.
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
    private final DevolucionService devolucionService;

    private final Map<Long, SesionUsuario> sesiones = new HashMap<>();

    private static final long TIMEOUT_MINUTOS = 30;

    // ============================================================
    // MÉTODO PRINCIPAL: PROCESAR MENSAJE (texto plano)
    // ============================================================
    public String procesarMensaje(String mensaje, Long chatId) {
        System.out.println("=== PROCESAR MENSAJE ===");
        System.out.println("ChatId: [" + chatId + "]");
        System.out.println("Mensaje: [" + mensaje + "]");

        String mensajeLower = mensaje.toLowerCase().trim();

        SesionUsuario sesion = sesiones.get(chatId);
        if (sesion == null) {
            sesion = new SesionUsuario();
            sesiones.put(chatId, sesion);
        }

        // ============ EXPIRACIÓN DE SESIÓN ============
        if (sesion.isAutenticado() && expiroSesion(sesion)) {
            sesiones.remove(chatId);
            return "⚠️ Tu sesión ha expirado por inactividad.\n\n" +
                    "📌 Han pasado más de 30 minutos sin actividad.\n\n" +
                    "👉 Ingresa nuevamente tu correo y contraseña.";
        }

        sesion.setUltimaActividad(LocalDateTime.now());

        // ============================================
        // CONVERSIÓN DE "1"/"2" A "si"/"no" CUANDO ESPERA CONFIRMACIÓN
        // Casos: solicitud subida, liquidación subida, o facturas subidas
        // ============================================
        if (sesion.isAutenticado()
                && sesion.getAccionPendiente() != null
                && (sesion.getFormularioSolicitud() != null
                || sesion.getFormularioLiquidacion() != null
                || sesion.isSubiendoFacturas())) {

            if (mensajeLower.equals("1")) {
                mensajeLower = "si";
                mensaje = "si";
            } else if (mensajeLower.equals("2")) {
                mensajeLower = "no";
                mensaje = "no";
            }
        }

        // ============================================
        // CLASIFICACIÓN CON IA (FASE A)
        // ============================================
        if (sesion.isAutenticado()
                && sesion.getAccionPendiente() == null
                && !sesion.isSubiendoFacturas()           // ← NUEVO
                && !sesion.isEsperandoConfirmacionCancelacion()
                && !sesion.isEsperandoConfirmacionCerrarSesion()
                && !sesion.isEsperandoMotivoRechazo()
                && !mensajeLower.startsWith("cerrar_sesion")
                && !mensajeLower.matches("\\d+")
                && !mensajeLower.matches("revisar\\s+\\d+")
                && !mensajeLower.contains("pendiente")
                && !mensajeLower.contains("revisar")
                && !mensajeLower.contains("transferencia")
                && !mensajeLower.contains("confirmar")
                && !mensajeLower.equals("listo")) {       // ← NUEVO {

            boolean esComandoDirecto = esComando(mensajeLower,
                    "menu", "menú", "inicio", "ayuda", "opciones", "help", "?",
                    "salir", "cancelar", "cancela", "volver", "atras", "atrás", "regresar",
                    "1", "2", "3", "4", "5", "6", "si", "sí", "no", "listo",
                    "pendientes", "pendiente", "revisar", "aprobar", "rechazar",
                    "registrar", "transferencia", "confirmar", "enviar", "devolver");

            if (!esComandoDirecto) {
                String intencion = openAIService.clasificarIntencion(mensaje);
                System.out.println("[IA] Mensaje: [" + mensaje + "] → Intención: " + intencion);

                switch (intencion) {
                    case "SOLICITAR":
                        mensajeLower = "solicitar";
                        mensaje = "solicitar";
                        break;
                    case "LIQUIDAR":
                        mensajeLower = "liquidar";
                        mensaje = "liquidar";
                        break;
                    case "CONSULTAR":
                        mensajeLower = "estado";
                        mensaje = "estado";
                        break;
                    case "AYUDA":
                        mensajeLower = "ayuda";
                        mensaje = "ayuda";
                        break;
                    case "SALIR":
                        mensajeLower = "salir";
                        mensaje = "salir";
                        break;
                    case "MENU":
                        mensajeLower = "menu";
                        mensaje = "menu";
                        break;
                    case "CANCELAR":
                        mensajeLower = "cancelar";
                        mensaje = "cancelar";
                        break;
                    case "CONSULTA_GENERAL":
                        return mensajeFueraDeAlcance(sesion);
                    case "DESCONOCIDA":
                        return mensajeFueraDeAlcance(sesion);
                }
            }
        }

        // ============ CONFIRMACIÓN DE CANCELACIÓN ============
        if (sesion.isEsperandoConfirmacionCancelacion()) {
            return procesarConfirmacionCancelacion(mensajeLower, sesion);
        }

        // ============ CONFIRMACIÓN DE CERRAR SESIÓN ============
        if (sesion.isEsperandoConfirmacionCerrarSesion()) {
            return procesarConfirmacionCerrarSesion(mensajeLower, sesion, chatId);
        }

        // ============================================
        // CALLBACKS DE BOTONES
        // ============================================

        if (mensajeLower.equals("cerrar_sesion_confirmar")) {
            sesion.setEsperandoConfirmacionCerrarSesion(true);
            return "⚠️ ¿Estás seguro de que quieres cerrar sesión?\n\n" +
                    "📌 Perderás el estado de la conversación actual.";
        }

        if (mensajeLower.equals("cerrar_sesion_si")) {
            sesiones.remove(chatId);
            return "👋 Sesión cerrada correctamente. Hasta luego.\n\n" +
                    "Para volver a ingresar, escribe *hola*.";
        }

        if (mensajeLower.equals("cerrar_sesion_no")) {
            sesion.setEsperandoConfirmacionCerrarSesion(false);
            return "👍 Perfecto, continúo con tu sesión.";
        }

        // ============================================
        // COMANDOS GENERALES
        // ============================================

        // SALIR
        if (esComando(mensajeLower, "salir", "cerrar sesion", "cerrar sesión", "logout")) {
            if (sesion.isAutenticado()) {
                sesion.setEsperandoConfirmacionCerrarSesion(true);
                return "⚠️ ¿Estás seguro de que quieres cerrar sesión?\n\n" +
                        "📌 Perderás el estado de la conversación actual.";
            }
        }

        // AYUDA
        if (esComando(mensajeLower, "ayuda", "opciones", "puedo hacer", "help", "?")) {
            if (sesion.isAutenticado()) {
                return menuAyudaContextual(sesion);
            } else {
                return "👋 Hola. Soy ITI, el asistente virtual del Instituto Tecnológico Internacional.\n\n" +
                        "👉 Para continuar, ingresa tu correo electrónico.";
            }
        }

        // MENÚ
        if (esComando(mensajeLower, "menu", "menú", "inicio", "volver al menu", "volver al menú")) {
            if (sesion.isAutenticado()) {
                limpiarOperacion(sesion);
                return menuPrincipal(sesion);
            }
        }

        // CANCELAR
        if (esComando(mensajeLower, "cancelar", "cancela", "abortar")) {
            if (sesion.isAutenticado() && sesion.getAccionPendiente() != null) {
                sesion.setEsperandoConfirmacionCancelacion(true);
                return "⚠️ ¿Estás seguro de cancelar la operación actual?\n\n" +
                        "📌 Operación en curso: *" + sesion.getAccionPendiente() + "*";
            }
            if (sesion.isAutenticado()) {
                return "ℹ️ No hay ninguna operación en curso.\n\n" + menuPrincipal(sesion);
            }
            return "ℹ️ No hay ninguna operación en curso.";
        }

        // VOLVER
        if (esComando(mensajeLower, "volver", "atras", "atrás", "regresar")) {
            if (sesion.isAutenticado()) {
                limpiarOperacion(sesion);
                return menuPrincipal(sesion);
            }
        }

        // LOGIN
        if (!sesion.isAutenticado()) {
            return manejarLogin(mensaje, mensajeLower, sesion);
        }

        // DELEGAR
        return delegarPorRol(mensaje, mensajeLower, sesion);
    }

    // ============================================================
    // PROCESAR MENSAJE CON BOTONES
    // ============================================================
    public ResponseBot procesarMensajeConBotones(String mensaje, Long chatId) {
        SesionUsuario sesionAntes = sesiones.get(chatId);
        boolean teniaFormularioSolicitud = sesionAntes != null && sesionAntes.getFormularioSolicitud() != null;
        boolean teniaFormularioLiquidacion = sesionAntes != null && sesionAntes.getFormularioLiquidacion() != null;
        boolean estabaSubiendoFacturas = sesionAntes != null && sesionAntes.isSubiendoFacturas();

        String texto = procesarMensaje(mensaje, chatId);

        SesionUsuario sesion = sesiones.get(chatId);

        if (sesion == null) {
            return new ResponseBot(texto);
        }

        List<List<ResponseBot.Boton>> botones = new ArrayList<>();

        if (sesion.isEsperandoConfirmacionCerrarSesion()) {
            botones.add(ResponseBot.fila(
                    ResponseBot.boton("✅ Sí, cerrar sesión", "cerrar_sesion_si"),
                    ResponseBot.boton("❌ No, continuar", "cerrar_sesion_no")
            ));
            return new ResponseBot(texto, botones);
        }

        if (sesion.isEsperandoConfirmacionCancelacion()) {
            botones.add(ResponseBot.fila(
                    ResponseBot.boton("✅ Sí, cancelar", "1"),
                    ResponseBot.boton("❌ No, continuar", "2")
            ));
            return new ResponseBot(texto, botones);
        }

        if (texto != null && texto.contains("¿Los datos son correctos?")
                && teniaFormularioSolicitud) {
            botones.add(ResponseBot.fila(
                    ResponseBot.boton("✅ Sí, continuar", "1"),
                    ResponseBot.boton("❌ No, corregir", "2")
            ));
            return new ResponseBot(texto, botones);
        }

        if (texto != null && texto.contains("¿Confirmas la liquidación?")
                && estabaSubiendoFacturas) {
            botones.add(ResponseBot.fila(
                    ResponseBot.boton("✅ Sí, continuar", "1"),
                    ResponseBot.boton("❌ No, corregir", "2")
            ));
            return new ResponseBot(texto, botones);
        }

        botones = determinarBotones(sesion);
        return new ResponseBot(texto, botones);
    }

    // ============================================================
    // PROCESAR DOCUMENTO CON BOTONES
    // ============================================================
    public ResponseBot procesarDocumentoConBotones(MultipartFile archivo, Long chatId) {
        String texto = procesarDocumento(archivo, chatId);

        SesionUsuario sesion = sesiones.get(chatId);
        if (sesion == null) {
            return new ResponseBot(texto);
        }

        if (texto != null) {
            if (texto.contains("¿Los datos son correctos?")) {
                List<List<ResponseBot.Boton>> botones = new ArrayList<>();
                botones.add(ResponseBot.fila(
                        ResponseBot.boton("✅ Sí, continuar", "1"),
                        ResponseBot.boton("❌ No, corregir", "2")
                ));
                return new ResponseBot(texto, botones);
            }

            if (texto.contains("¿Confirmas la liquidación?")) {
                List<List<ResponseBot.Boton>> botones = new ArrayList<>();
                botones.add(ResponseBot.fila(
                        ResponseBot.boton("✅ Sí, continuar", "1"),
                        ResponseBot.boton("❌ No, corregir", "2")
                ));
                return new ResponseBot(texto, botones);
            }
        }

        List<List<ResponseBot.Boton>> botones = determinarBotones(sesion);
        return new ResponseBot(texto, botones);
    }

    // ============================================================
    // DETERMINAR BOTONES SEGÚN ESTADO
    // ============================================================
    private List<List<ResponseBot.Boton>> determinarBotones(SesionUsuario sesion) {
        List<List<ResponseBot.Boton>> botones = new ArrayList<>();

        if (sesion.isEsperandoConfirmacionCerrarSesion()) {
            botones.add(ResponseBot.fila(
                    ResponseBot.boton("✅ Sí, cerrar sesión", "cerrar_sesion_si"),
                    ResponseBot.boton("❌ No, continuar", "cerrar_sesion_no")
            ));
            return botones;
        }

        if (sesion.isEsperandoConfirmacionCancelacion()) {
            botones.add(ResponseBot.fila(
                    ResponseBot.boton("✅ Sí, cancelar", "1"),
                    ResponseBot.boton("❌ No, continuar", "2")
            ));
            return botones;
        }

        if (sesion.isAutenticado()) {
            String rol = sesion.getRolNombre();

            // DOCENTE CON FORMULARIO SUBIDO
            if (rol != null && rol.equalsIgnoreCase("Docente")
                    && "SOLICITAR".equals(sesion.getAccionPendiente())
                    && sesion.getFormularioSolicitud() != null) {
                botones.add(ResponseBot.fila(
                        ResponseBot.boton("✅ Sí, continuar", "1"),
                        ResponseBot.boton("❌ No, corregir", "2")
                ));
                return botones;
            }

            if (rol != null && rol.equalsIgnoreCase("Docente")
                    && "LIQUIDAR".equals(sesion.getAccionPendiente())
                    && sesion.getFormularioLiquidacion() != null
                    && !sesion.isSubiendoFacturas()) {
                botones.add(ResponseBot.fila(
                        ResponseBot.boton("✅ Sí, continuar", "1"),
                        ResponseBot.boton("❌ No, corregir", "2")
                ));
                return botones;
            }

            // DOCENTE EN MENÚ PRINCIPAL
            if (rol != null && rol.equalsIgnoreCase("Docente")
                    && sesion.getAccionPendiente() == null) {
                botones.add(ResponseBot.fila(
                        ResponseBot.boton("📝 Solicitar anticipo", "1"),
                        ResponseBot.boton("💰 Liquidar anticipo", "2")
                ));
                botones.add(ResponseBot.fila(
                        ResponseBot.boton("📋 Consultar estado", "3"),
                        ResponseBot.boton("❓ Ayuda", "5")
                ));
                botones.add(ResponseBot.fila(
                        ResponseBot.boton("🚪 Cerrar sesión", "cerrar_sesion_confirmar")
                ));
                return botones;
            }

            // DOCENTE SUBIENDO FACTURAS
            if (rol != null && rol.equalsIgnoreCase("Docente")
                    && sesion.isSubiendoFacturas()) {
                botones.add(ResponseBot.fila(
                        ResponseBot.boton("✅ Terminar y calcular saldo", "listo")
                ));
                botones.add(ResponseBot.fila(
                        ResponseBot.boton("❌ Cancelar operación", "cancelar")
                ));
                return botones;
            }

            // DOCENTE CON DEVOLUCIÓN PENDIENTE
            if (rol != null && rol.equalsIgnoreCase("Docente")
                    && "DEVOLUCION".equals(sesion.getAccionPendiente())) {
                botones.add(ResponseBot.fila(
                        ResponseBot.boton("📎 Subir comprobante", "subir_comprobante")
                ));
                botones.add(ResponseBot.fila(
                        ResponseBot.boton("❌ Cancelar", "cancelar")
                ));
                return botones;
            }

// DOCENTE EN OPERACIÓN (esperando PDF)
            if (rol != null && rol.equalsIgnoreCase("Docente")
                    && sesion.getAccionPendiente() != null) {
                botones.add(ResponseBot.fila(
                        ResponseBot.boton("❌ Cancelar operación", "cancelar")
                ));
                return botones;
            }

            // APROBADORES EN MENÚ
            if (rol != null && (rol.equalsIgnoreCase("Jefe Inmediato")
                    || rol.equalsIgnoreCase("Director Financiero")
                    || rol.equalsIgnoreCase("Contadora")
                    || rol.equalsIgnoreCase("Colectora")
                    || rol.equalsIgnoreCase("Asistente Contable"))
                    && sesion.getTramiteEnRevision() == null
                    && !sesion.isEsperandoMotivoRechazo()) {
                botones.add(ResponseBot.fila(
                        ResponseBot.boton("📋 Ver pendientes", "pendientes")
                ));
                botones.add(ResponseBot.fila(
                        ResponseBot.boton("❓ Ayuda", "5"),
                        ResponseBot.boton("🚪 Cerrar sesión", "cerrar_sesion_confirmar")
                ));
                return botones;
            }

            // REVISANDO UN TRÁMITE — botones según el rol
            if (sesion.getTramiteEnRevision() != null && !sesion.isEsperandoMotivoRechazo()) {

                if (rol != null && rol.equalsIgnoreCase("Colectora")) {
                    botones.add(ResponseBot.fila(
                            ResponseBot.boton("💸 Registrar transferencia", "confirmar transferencia")
                    ));
                    botones.add(ResponseBot.fila(
                            ResponseBot.boton("🏠 Menú principal", "menu")
                    ));
                    return botones;
                }

                if (rol != null && rol.equalsIgnoreCase("Asistente Contable")) {
                    botones.add(ResponseBot.fila(
                            ResponseBot.boton("📤 Enviar a Contadora", "enviar"),
                            ResponseBot.boton("↩️ Devolver al docente", "devolver")
                    ));
                    botones.add(ResponseBot.fila(
                            ResponseBot.boton("🏠 Menú principal", "menu")
                    ));
                    return botones;
                }

                botones.add(ResponseBot.fila(
                        ResponseBot.boton("✅ Aprobar", "aprobar"),
                        ResponseBot.boton("❌ Rechazar", "rechazar")
                ));
                botones.add(ResponseBot.fila(
                        ResponseBot.boton("🏠 Menú principal", "menu")
                ));
                return botones;
            }
        }

        return botones;
    }

    // ============================================================
    // CONFIRMACIÓN DE CANCELACIÓN DE OPERACIÓN
    // ============================================================
    private String procesarConfirmacionCancelacion(String mensajeLower, SesionUsuario sesion) {
        if (esComando(mensajeLower, "1", "si", "sí", "s", "yes", "confirmar", "confirmo")) {
            sesion.setEsperandoConfirmacionCancelacion(false);
            limpiarOperacion(sesion);
            return "✅ Operación cancelada.\n\n" + menuPrincipal(sesion);
        }

        if (esComando(mensajeLower, "2", "no", "n", "continuar")) {
            sesion.setEsperandoConfirmacionCancelacion(false);
            return "👍 Perfecto, continúo con tu operación.";
        }

        return "⚠️ No reconocí tu respuesta.\n\n" +
                "👉 Usa los botones para responder.";
    }

    // ============================================================
    // CONFIRMACIÓN DE CERRAR SESIÓN
    // ============================================================
    private String procesarConfirmacionCerrarSesion(String mensajeLower, SesionUsuario sesion, Long chatId) {
        if (esComando(mensajeLower, "cerrar_sesion_si", "1", "si", "sí", "s", "yes", "confirmar", "confirmo")) {
            sesiones.remove(chatId);
            return "👋 Sesión cerrada correctamente. Hasta luego.\n\n" +
                    "Para volver a ingresar, escribe *hola*.";
        }

        if (esComando(mensajeLower, "cerrar_sesion_no", "2", "no", "n", "continuar")) {
            sesion.setEsperandoConfirmacionCerrarSesion(false);
            return "👍 Perfecto, continúo con tu sesión.\n\n" + menuPrincipal(sesion);
        }

        return "⚠️ No reconocí tu respuesta.\n\n" +
                "👉 Usa los botones para responder.";
    }

    // ============================================================
    // AYUDA CONTEXTUAL
    // ============================================================
    private String menuAyudaContextual(SesionUsuario sesion) {
        String accion = sesion.getAccionPendiente();
        String rol = sesion.getRolNombre();

        if (rol.equalsIgnoreCase("Docente")) {
            if ("SOLICITAR".equals(accion) && sesion.getFormularioSolicitud() == null) {
                return "ℹ️ *Ayuda — Esperando formulario*\n\n" +
                        "📌 En este momento necesitas enviar el formulario de solicitud de anticipo.\n\n" +
                        "👉 Adjúntalo en formato PDF desde esta conversación.";
            }
            if ("SOLICITAR".equals(accion) && sesion.getFormularioSolicitud() != null) {
                return "ℹ️ *Ayuda — Confirmando solicitud*\n\n" +
                        "📌 Ya extraje los datos del formulario.\n\n" +
                        "👉 Usa los botones para confirmar o corregir.";
            }
            if ("LIQUIDAR".equals(accion) && sesion.getFormularioLiquidacion() == null) {
                return "ℹ️ *Ayuda — Esperando formulario*\n\n" +
                        "📌 En este momento necesitas enviar el formulario de liquidación.\n\n" +
                        "👉 Adjúntalo en formato PDF.";
            }
            if ("LIQUIDAR".equals(accion) && sesion.isSubiendoFacturas()) {
                return "ℹ️ *Ayuda — Subiendo facturas*\n\n" +
                        "📌 Estás subiendo las facturas una por una.\n\n" +
                        "👉 Sube cada factura en PDF.\n" +
                        "✅ Cuando termines, escribe *listo*.\n\n" +
                        "📊 Llevas *" + sesion.getFacturasExtraidas().size() + "* facturas registradas.";
            }
            if ("DEVOLUCION".equals(accion)) {
                return "ℹ️ *Ayuda — Devolución*\n\n" +
                        "📌 Estás registrando una devolución.\n\n" +
                        "👉 Sube el comprobante de depósito en PDF.";
            }
            return "ℹ️ *Ayuda — Docente*\n\n" +
                    "Puedo ayudarte con:\n" +
                    "📝 Solicitar un anticipo\n" +
                    "💰 Liquidar un anticipo\n" +
                    "📋 Consultar el estado de tus trámites";
        }

        if (sesion.isEsperandoMotivoRechazo()) {
            return "ℹ️ *Ayuda — Motivo de rechazo*\n\n" +
                    "📌 Estás escribiendo el motivo del rechazo.\n\n" +
                    "👉 Escribe el motivo y se enviará como observación.";
        }

        if (sesion.getTramiteEnRevision() != null) {
            return "ℹ️ *Ayuda — Revisando trámite*\n\n" +
                    "📌 Estás revisando el trámite *" + sesion.getTramiteEnRevision() + "*.\n\n" +
                    "👉 Usa los botones para aprobar o rechazar.";
        }

        return "ℹ️ *Ayuda — " + rol + "*\n\n" +
                "👉 Usa los botones del menú para navegar.";
    }

    // ============================================================
    // DOCUMENTOS
    // ============================================================
    public String procesarDocumento(MultipartFile archivo, Long chatId) {
        SesionUsuario sesion = sesiones.get(chatId);

        if (sesion == null || !sesion.isAutenticado()) {
            return "⚠️ Debes iniciar sesión primero.\n\n" +
                    "👉 Escribe *hola* para comenzar.";
        }

        if (sesion.isEsperandoConfirmacionCancelacion()) {
            return "⚠️ Tienes una pregunta pendiente.\n\n" +
                    "👉 Responde con los botones antes de enviar documentos.";
        }

        if (sesion.isProcesandoDocumento()) {
            return "⏳ Todavía estoy procesando el documento anterior.\n\n" +
                    "📌 Espera a que termine antes de enviar otro.";
        }

        sesion.setProcesandoDocumento(true);

        try {
            if (sesion.getRolNombre().equalsIgnoreCase("Docente")) {
                return docenteChatService.procesarDocumento(archivo, sesion);
            }
            return "ℹ️ Tu rol no permite subir documentos.";
        } finally {
            sesion.setProcesandoDocumento(false);
        }
    }

    // ============================================================
    // HELPERS
    // ============================================================
    private boolean esComando(String mensajeLower, String... opciones) {
        for (String op : opciones) {
            if (mensajeLower.equals(op)) return true;
        }
        return false;
    }

    private void limpiarOperacion(SesionUsuario sesion) {
        sesion.setAccionPendiente(null);
        sesion.setFormularioSolicitud(null);
        sesion.setFormularioLiquidacion(null);
        sesion.setSubiendoFacturas(false);
        sesion.getFacturasExtraidas().clear();
        sesion.setTramiteEnRevision(null);
        sesion.setEsperandoMotivoRechazo(false);
        sesion.setEsperandoConfirmacionCancelacion(false);
    }

    // ============================================================
    // MENÚ PRINCIPAL
    // ============================================================
    private String menuPrincipal(SesionUsuario sesion) {
        return "¿Qué necesitas hacer hoy?";
    }

    // ============================================================
    // DELEGACIÓN POR ROL
    // ============================================================
    private String delegarPorRol(String mensaje, String mensajeLower, SesionUsuario sesion) {
        String rol = sesion.getRolNombre();

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
                        || mensajeLower.contains("menú") || mensajeLower.contains("cancelar");

                if (esConfirmacion) {
                    // No bloquear
                } else {
                    boolean esTexto = !mensajeLower.contains(".pdf")
                            && !mensajeLower.startsWith("http");

                    if (esTexto) {
                        switch (accion) {
                            case "SOLICITAR":
                                return "⚠️ El archivo enviado no es un PDF.\n\n" +
                                        "📌 En este paso necesito el formulario de solicitud de anticipo.\n\n" +
                                        "👉 Adjúntalo nuevamente en formato PDF.";
                            case "LIQUIDAR":
                                return "⚠️ El archivo enviado no es un PDF.\n\n" +
                                        "📌 En este paso necesito el formulario de liquidación.\n\n" +
                                        "👉 Adjúntalo nuevamente en formato PDF.";
                            case "FACTURAS":
                                return "⚠️ El archivo enviado no es un PDF.\n\n" +
                                        "📌 En este paso necesito las facturas.\n\n" +
                                        "👉 Adjúntalas nuevamente en formato PDF.";
                            case "DEVOLUCION":
                                return "⚠️ El archivo enviado no es un PDF.\n\n" +
                                        "📌 En este paso necesito el comprobante de devolución.\n\n" +
                                        "👉 Adjúntalo nuevamente en formato PDF.";
                        }
                    }
                }
            }
        }

        if (rol.equalsIgnoreCase("Docente")) {
            String respuesta = docenteChatService.procesar(mensaje, mensajeLower, sesion);
            if (respuesta != null) return respuesta;
        }

        if (rol.equalsIgnoreCase("Jefe Inmediato")) {
            ConfiguracionAprobador config = new ConfiguracionAprobador(
                    "Pendiente", "Aprobado por Jefe",
                    "carlos.ruiz@iti.edu.ec", "Jefe", "juan.perez@iti.edu.ec",
                    "Director Financiero");

            String respuesta = aprobadorChatService.procesar(mensaje, mensajeLower, sesion, config);
            if (respuesta != null) return respuesta;
        }

        if (rol.equalsIgnoreCase("Director Financiero")) {
            ConfiguracionAprobador config = new ConfiguracionAprobador(
                    "Aprobado por Jefe", "Aprobado por Director",
                    "ana.torres@iti.edu.ec", "Director", "juan.perez@iti.edu.ec",
                    "Contadora");

            String respuesta = aprobadorChatService.procesar(mensaje, mensajeLower, sesion, config);
            if (respuesta != null) return respuesta;
        }

        if (rol.equalsIgnoreCase("Contadora")) {
            String respuesta = contadoraChatService.procesar(mensaje, mensajeLower, sesion);
            if (respuesta != null) return respuesta;
        }

        if (rol.equalsIgnoreCase("Colectora")) {
            String respuesta = colectoraChatService.procesar(mensaje, mensajeLower, sesion);
            if (respuesta != null) return respuesta;
        }

        if (rol.equalsIgnoreCase("Asistente Contable")) {
            String respuesta = asistenteContableChatService.procesar(mensaje, mensajeLower, sesion);
            if (respuesta != null) return respuesta;
        }

        if (rol.equalsIgnoreCase("Administrador")) {
            if (mensajeLower.contains("usuario")) {
                return "ℹ️ Mostrando gestión de usuarios...";
            }
            if (mensajeLower.contains("historial")) {
                return "ℹ️ Mostrando historial general...";
            }
        }

        return mensajeFueraDeAlcance(sesion);
    }

    // ============================================================
    // LOGIN
    // ============================================================
    private String manejarLogin(String mensaje, String mensajeLower, SesionUsuario sesion) {

        if (!sesion.isEsperandoContrasena()) {
            String correo = mensaje.trim();

            if (!correo.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
                return "👋 Hola. Soy ITI, el asistente virtual del Instituto Tecnológico Internacional.\n\n" +
                        "👉 Para continuar, ingresa tu correo electrónico.";
            }

            if (!usuarioService.existeCorreo(correo)) {
                return "⚠️ El correo no está registrado en el sistema.\n\n" +
                        "📌 Verifica que esté bien escrito.\n\n" +
                        "👉 Intenta nuevamente con tu correo institucional.";
            }

            sesion.setCorreo(correo);
            sesion.setEsperandoContrasena(true);

            return "✅ Correo verificado.\n\n" +
                    "👉 Ahora ingresa tu contraseña.";
        }

        LoginRequestDTO loginDTO = new LoginRequestDTO();
        loginDTO.setCorreo(sesion.getCorreo());
        loginDTO.setContrasena(mensaje.trim());

        LoginResponseDTO response = usuarioService.login(loginDTO);

        if (response.isBloqueado()) {
            String correoBloqueado = sesion.getCorreo();
            sesion.setEsperandoContrasena(false);
            sesion.setCorreo(null);

            return "🔒 *Cuenta bloqueada*\n\n" +
                    "📌 La cuenta *" + correoBloqueado + "* está bloqueada por seguridad.\n\n" +
                    "📞 Contacta al departamento de sistemas del ITI:\n" +
                    "• Teléfono: (02) 123-4567\n" +
                    "• Correo: soporte@iti.edu.ec\n\n" +
                    "Cuando soporte desbloquee tu cuenta, escribe *hola* para volver a intentar.";
        }

        if (response.isAutenticado()) {
            sesion.setUsuarioId(response.getUsuarioId());
            sesion.setNombre(response.getNombre());
            sesion.setRolNombre(response.getRolNombre());
            sesion.setAutenticado(true);
            sesion.setEsperandoContrasena(false);

            String bienvenida = "👋 Hola *" + response.getNombre() + "*. Soy ITI, tu asistente virtual.\n\n";

            // ✅ Si es Docente, verificar si tiene devoluciones pendientes
            if (response.getRolNombre().equalsIgnoreCase("Docente")) {
                String avisoDevolucion = verificarDevolucionPendiente(response.getUsuarioId(), sesion);
                if (avisoDevolucion != null) {
                    return bienvenida + avisoDevolucion;
                }
            }

            return bienvenida + menuPrincipal(sesion);
        }

        return "⚠️ Contraseña incorrecta.\n\n" +
                "📌 El correo *" + sesion.getCorreo() + "* es válido, pero la contraseña no coincide.\n\n" +
                "🔢 Intentos restantes: *" + response.getIntentosRestantes() + "*\n\n" +
                "👉 Ingresa tu contraseña nuevamente.";
    }

    // ============================================================
    // UTILIDADES
    // ============================================================
    private boolean expiroSesion(SesionUsuario sesion) {
        return Duration.between(sesion.getUltimaActividad(), LocalDateTime.now())
                .toMinutes() > TIMEOUT_MINUTOS;
    }

    // ============================================================
    // MENSAJE FUERA DE ALCANCE SEGÚN ROL
    // ============================================================
    private String mensajeFueraDeAlcance(SesionUsuario sesion) {
        String rol = sesion.getRolNombre();

        if (rol == null) {
            return "🤔 Esa pregunta está fuera de mi alcance.\n\n" +
                    "📌 Soy un asistente especializado en la gestión de anticipos del ITI.";
        }

        if (rol.equalsIgnoreCase("Docente")) {
            return "🤔 Esa pregunta está fuera de mi alcance.\n\n" +
                    "📌 Soy un asistente especializado en la gestión de anticipos del ITI.\n\n" +
                    "Puedo ayudarte con:\n" +
                    "📝 Solicitar un anticipo\n" +
                    "💰 Liquidar un anticipo\n" +
                    "📋 Consultar el estado de tus trámites\n\n" +
                    "👉 Usa los botones para continuar.";
        }

        if (rol.equalsIgnoreCase("Jefe Inmediato") || rol.equalsIgnoreCase("Director Financiero")) {
            return "🤔 Esa pregunta está fuera de mi alcance.\n\n" +
                    "📌 Soy un asistente especializado en la gestión de anticipos del ITI.\n\n" +
                    "Puedo ayudarte con:\n" +
                    "📋 Revisar trámites pendientes\n" +
                    "✅ Aprobar o rechazar trámites\n\n" +
                    "👉 Usa los botones para continuar.";
        }

        if (rol.equalsIgnoreCase("Contadora")) {
            return "🤔 Esa pregunta está fuera de mi alcance.\n\n" +
                    "📌 Soy un asistente especializado en la gestión de anticipos del ITI.\n\n" +
                    "Puedo ayudarte con:\n" +
                    "📋 Revisar solicitudes y liquidaciones\n" +
                    "✅ Aprobar o rechazar trámites\n\n" +
                    "👉 Usa los botones para continuar.";
        }

        if (rol.equalsIgnoreCase("Colectora")) {
            return "🤔 Esa pregunta está fuera de mi alcance.\n\n" +
                    "📌 Soy un asistente especializado en la gestión de anticipos del ITI.\n\n" +
                    "Puedo ayudarte con:\n" +
                    "📋 Ver transferencias pendientes\n" +
                    "💸 Registrar transferencias\n\n" +
                    "👉 Usa los botones para continuar.";
        }

        if (rol.equalsIgnoreCase("Asistente Contable")) {
            return "🤔 Esa pregunta está fuera de mi alcance.\n\n" +
                    "📌 Soy un asistente especializado en la gestión de anticipos del ITI.\n\n" +
                    "Puedo ayudarte con:\n" +
                    "📋 Revisar liquidaciones pendientes\n" +
                    "📤 Enviar liquidaciones a la Contadora\n\n" +
                    "👉 Usa los botones para continuar.";
        }

        if (rol.equalsIgnoreCase("Administrador")) {
            return "🤔 Esa pregunta está fuera de mi alcance.\n\n" +
                    "📌 Soy un asistente especializado en la gestión de anticipos del ITI.\n\n" +
                    "👉 Usa los botones para continuar.";
        }

        return "🤔 Esa pregunta está fuera de mi alcance.\n\n" +
                "📌 Soy un asistente especializado en la gestión de anticipos del ITI.";
    }

    // ============================================================
// VERIFICAR DEVOLUCIÓN PENDIENTE
// ============================================================
    private String verificarDevolucionPendiente(Long usuarioId, SesionUsuario sesion) {
        try {
            Optional<DevolucionResponseDTO> devolucionOpt = devolucionService.buscarPendientePorUsuario(usuarioId);

            if (devolucionOpt.isPresent()) {
                DevolucionResponseDTO devolucion = devolucionOpt.get();

                // Activar el modo DEVOLUCION en la sesión
                sesion.setAccionPendiente("DEVOLUCION");

                return "⚠️ *Tienes una devolución pendiente*\n\n" +
                        "💰 Monto a devolver: $" + devolucion.getMonto() + "\n" +
                        "🆔 Trámite: #" + devolucion.getTramiteId() + "\n\n" +
                        "📎 Sube el comprobante de transferencia en PDF.\n\n" +
                        "👉 Adjúntalo en este chat.";
            }
        } catch (Exception e) {
            System.err.println("Error al verificar devolución: " + e.getMessage());
        }
        return null;
    }
}