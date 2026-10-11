package ec.edu.iti.anticipos.chatbot.service.chat;

import ec.edu.iti.anticipos.chatbot.dto.request.HistorialEstadoRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.request.NotificacionRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.request.OrdenTransferenciaRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.OrdenTransferenciaResponseDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.ReembolsoResponseDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.TramiteDetalleDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.TramiteResponseDTO;
import ec.edu.iti.anticipos.chatbot.service.*;
import ec.edu.iti.anticipos.chatbot.sesion.SesionUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Servicio común para los roles aprobadores: Jefe, Director y Contadora.
 *
 * ⚠️ ADEMÁS, el Director Financiero puede:
 *  - Aprobar órdenes de transferencia (anticipos).
 *  - AUTORIZAR REEMBOLSOS (caso FALTANTE).
 */
@Service
@RequiredArgsConstructor
public class AprobadorChatService {

    private final TramiteService tramiteService;
    private final NotificacionService notificacionService;
    private final HistorialEstadoService historialEstadoService;
    private final OrdenTransferenciaService ordenTransferenciaService;
    private final ReembolsoService reembolsoService;
    private final UsuarioService usuarioService;

    private static final int MAX_PROPOSITO = 30;

    // ============================================================
    // PROCESAR MENSAJE
    // ============================================================
    public String procesar(String mensaje, String mensajeLower, SesionUsuario sesion,
                           ConfiguracionAprobador config) {

        // ============================================================
        // REEMBOLSOS (solo Director Financiero)
        // ============================================================

        // Autorizar reembolso específico: "autorizar reembolso 5"
        if (mensajeLower.matches("autorizar\\s+reembolso\\s+\\d+")) {
            Long reembolsoId = Long.parseLong(mensajeLower.replaceAll("[^0-9]", ""));
            return autorizarReembolso(reembolsoId, sesion);
        }

        // Listar reembolsos pendientes: "reembolsos"
        if (sesion.getRolNombre() != null
                && sesion.getRolNombre().equalsIgnoreCase("Director Financiero")
                && (mensajeLower.equals("reembolsos") || mensajeLower.equals("reembolso"))) {
            return listarReembolsosPendientes();
        }

        // ============================================================
        // ÓRDENES DE TRANSFERENCIA (solo Director Financiero)
        // ============================================================

        // Aprobar orden de transferencia
        if (mensajeLower.matches("aprobar\\s+orden\\s+\\d+")) {
            Long ordenId = Long.parseLong(mensajeLower.replaceAll("[^0-9]", ""));
            return aprobarOrden(ordenId, sesion);
        }

        // Listar órdenes pendientes
        if (sesion.getRolNombre() != null
                && sesion.getRolNombre().equalsIgnoreCase("Director Financiero")
                && (mensajeLower.equals("ordenes") || mensajeLower.contains("orden"))) {
            return listarOrdenesPendientes();
        }

        // ============================================================
        // APROBACIÓN DE TRÁMITES (comportamiento original)
        // ============================================================

        if (sesion.isEsperandoMotivoRechazo()) {
            return procesarMotivoRechazo(mensaje, sesion, config);
        }

        if (mensajeLower.matches("revisar\\s+\\d+")) {
            return seleccionarTramite(mensaje, sesion);
        }

        if (mensajeLower.matches("\\d+")) {
            return seleccionarTramitePorId(Long.parseLong(mensajeLower), sesion);
        }

        if (mensajeLower.contains("pendiente") || mensajeLower.contains("revisar")) {
            return listarPendientes(sesion, config);
        }

        if (mensajeLower.contains("aprobar")) {
            return aprobar(sesion, config);
        }

        if (mensajeLower.contains("rechazar")) {
            return iniciarRechazo(sesion);
        }

        return null;
    }

    // ============================================================
    // LISTAR TRÁMITES PENDIENTES
    // ============================================================
    private String listarPendientes(SesionUsuario sesion, ConfiguracionAprobador config) {
        List<TramiteResponseDTO> pendientes = tramiteService.listarPorEstado(config.getEstadoBuscado());

        if (pendientes.isEmpty()) {
            return "📋 No hay trámites pendientes de revisión.\n\n" +
                    "👉 Escribe *menu* para volver al menú principal.";
        }

        StringBuilder lista = new StringBuilder("📋 *Solicitudes de anticipo pendientes:* (")
                .append(pendientes.size()).append(")\n\n");

        for (TramiteResponseDTO t : pendientes) {
            lista.append("🆔 ").append(t.getTramiteId())
                    .append(" — ").append(t.getNombreUsuario());

            Optional<TramiteDetalleDTO> detalleOpt = tramiteService.obtenerDetalle(t.getTramiteId());
            if (detalleOpt.isPresent() && detalleOpt.get().getProposito() != null) {
                String proposito = detalleOpt.get().getProposito();
                if (proposito.length() > MAX_PROPOSITO) {
                    proposito = proposito.substring(0, MAX_PROPOSITO) + "...";
                }
                lista.append(" · ").append(proposito);
            }

            lista.append("\n");
        }

        lista.append("\n👉 Escribe *revisar ID* o solo el número para ver el detalle.\n");
        lista.append("Ejemplo: revisar 15 o 15");
        return lista.toString();
    }

    // ============================================================
    // SELECCIONAR TRÁMITE
    // ============================================================
    private String seleccionarTramite(String mensaje, SesionUsuario sesion) {
        try {
            Long tramiteId = Long.parseLong(mensaje.replaceAll("[^0-9]", ""));
            return seleccionarTramitePorId(tramiteId, sesion);
        } catch (NumberFormatException e) {
            return "⚠️ Formato incorrecto.\n\n" +
                    "📌 Debes escribir *revisar* seguido del número.\n\n" +
                    "👉 Ejemplo: revisar 15";
        }
    }

    private String seleccionarTramitePorId(Long tramiteId, SesionUsuario sesion) {
        sesion.setTramiteEnRevision(tramiteId);

        Optional<TramiteDetalleDTO> detalleOpt = tramiteService.obtenerDetalle(tramiteId);

        if (detalleOpt.isEmpty()) {
            return "⚠️ No encontré el trámite *" + tramiteId + "*.\n\n" +
                    "📌 Verifica que el ID sea correcto.";
        }

        TramiteDetalleDTO detalle = detalleOpt.get();

        StringBuilder sb = new StringBuilder();
        sb.append("📋 *Trámite #").append(detalle.getTramiteId()).append("*\n\n");
        sb.append("👤 Solicitante: ").append(detalle.getNombreUsuario()).append("\n");
        sb.append("📝 Tipo: ").append(detalle.getNombreTipoTramite()).append("\n");
        sb.append("📅 Fecha: ").append(detalle.getFechaCreacion()).append("\n");
        sb.append("📊 Estado: ").append(detalle.getEstado()).append("\n");

        if (detalle.getPresupuesto() != null) {
            sb.append("\n💰 Presupuesto: $").append(detalle.getPresupuesto()).append("\n");
            if (detalle.getUnidad() != null) {
                sb.append("🏢 Unidad: ").append(detalle.getUnidad()).append("\n");
            }
            if (detalle.getCampus() != null) {
                sb.append("📍 Campus: ").append(detalle.getCampus()).append("\n");
            }
            if (detalle.getProposito() != null) {
                sb.append("🎯 Propósito: ").append(detalle.getProposito()).append("\n");
            }
            if (detalle.getLugar() != null) {
                sb.append("📌 Lugar: ").append(detalle.getLugar()).append("\n");
            }
            if (detalle.getFechaRealizacion() != null) {
                sb.append("📅 Fecha realización: ").append(detalle.getFechaRealizacion()).append("\n");
            }
        }

        if (detalle.getTotalGastado() != null) {
            sb.append("\n💰 Total gastado: $").append(detalle.getTotalGastado()).append("\n");
            if (detalle.getSaldo() != null) {
                sb.append("📈 Saldo: $").append(detalle.getSaldo()).append("\n");
            }
        }

        sb.append("\n¿Qué quieres hacer?");

        return sb.toString();
    }

    // ============================================================
    // APROBAR TRÁMITE
    // ============================================================
    private String aprobar(SesionUsuario sesion, ConfiguracionAprobador config) {
        if (sesion.getTramiteEnRevision() == null) {
            return "⚠️ No has seleccionado ningún trámite.\n\n" +
                    "📌 Primero debes elegir uno de la lista.\n\n" +
                    "👉 Escribe *revisar ID* o solo el número para seleccionarlo.";
        }

        Long tramiteId = sesion.getTramiteEnRevision();
        String estadoAnterior = config.getEstadoBuscado();
        String nuevoEstado = config.getNuevoEstado();

        tramiteService.actualizarEstado(tramiteId, nuevoEstado);

        HistorialEstadoRequestDTO historialDTO = new HistorialEstadoRequestDTO();
        historialDTO.setTramiteId(tramiteId);
        historialDTO.setEstadoAnterior(estadoAnterior);
        historialDTO.setEstadoNuevo(nuevoEstado);
        historialDTO.setUsuarioId(sesion.getUsuarioId());
        historialDTO.setObservacion("Aprobado por " + config.getRolNombre());
        historialEstadoService.guardar(historialDTO);

        // NOTIFICAR AL SIGUIENTE ROL (dinámico)
        String correoSiguiente = usuarioService.obtenerCorreoPorRol(config.getNombreSiguienteRol());

        if (correoSiguiente == null) {
            System.err.println("⚠️ No se encontró un usuario con el rol: " + config.getNombreSiguienteRol());
        } else {
            NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
            notifDTO.setTramiteId(tramiteId);
            notifDTO.setDestinatario(correoSiguiente);
            notifDTO.setTipo("Trámite aprobado por " + config.getRolNombre());
            notificacionService.guardar(notifDTO);
        }

        sesion.setTramiteEnRevision(null);

        return "✅ Trámite *" + tramiteId + "* aprobado correctamente.\n\n" +
                "📬 Se ha notificado al *" + config.getNombreSiguienteRol() + "*.";
    }

    // ============================================================
    // RECHAZAR TRÁMITE
    // ============================================================
    private String iniciarRechazo(SesionUsuario sesion) {
        if (sesion.getTramiteEnRevision() == null) {
            return "⚠️ No has seleccionado ningún trámite.\n\n" +
                    "📌 Primero debes elegir uno de la lista.\n\n" +
                    "👉 Escribe *revisar ID* o solo el número para seleccionarlo.";
        }
        sesion.setEsperandoMotivoRechazo(true);
        return "✍️ ¿Cuál es el motivo del rechazo?\n\n" +
                "👉 Escribe el motivo y se enviará al funcionario.";
    }

    private String procesarMotivoRechazo(String motivo, SesionUsuario sesion,
                                         ConfiguracionAprobador config) {
        sesion.setMotivoRechazo(motivo);
        sesion.setEsperandoMotivoRechazo(false);

        Long tramiteId = sesion.getTramiteEnRevision();
        String estadoAnterior = config.getEstadoBuscado();

        tramiteService.actualizarEstado(tramiteId, "Rechazado");

        HistorialEstadoRequestDTO historialDTO = new HistorialEstadoRequestDTO();
        historialDTO.setTramiteId(tramiteId);
        historialDTO.setEstadoAnterior(estadoAnterior);
        historialDTO.setEstadoNuevo("Rechazado");
        historialDTO.setUsuarioId(sesion.getUsuarioId());
        historialDTO.setObservacion("Rechazado por " + config.getRolNombre() + ": " + motivo);
        historialEstadoService.guardar(historialDTO);

        // NOTIFICAR AL FUNCIONARIO ORIGINAL (dinámico)
        String correoFuncionario = tramiteService.obtenerDetalle(tramiteId)
                .map(TramiteDetalleDTO::getCorreoUsuario)
                .orElse(null);

        if (correoFuncionario != null) {
            NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
            notifDTO.setTramiteId(tramiteId);
            notifDTO.setDestinatario(correoFuncionario);
            notifDTO.setTipo("Trámite rechazado por " + config.getRolNombre());
            notificacionService.guardar(notifDTO);
        }

        sesion.setTramiteEnRevision(null);
        sesion.setMotivoRechazo(null);

        return "❌ Trámite *" + tramiteId + "* rechazado.\n\n" +
                "📌 Motivo: " + motivo + "\n\n" +
                "📬 Se ha notificado al *Funcionario*.";
    }

    // ============================================================
    // ÓRDENES DE TRANSFERENCIA
    // ============================================================
    private String listarOrdenesPendientes() {
        List<OrdenTransferenciaResponseDTO> ordenes = ordenTransferenciaService.listarPorEstado("Generada");

        if (ordenes.isEmpty()) {
            return "📋 No hay órdenes de transferencia pendientes de aprobación.\n\n" +
                    "👉 Escribe *menu* para volver al menú principal.";
        }

        StringBuilder lista = new StringBuilder("📋 *Órdenes de transferencia pendientes:* (" + ordenes.size() + ")\n\n");
        for (OrdenTransferenciaResponseDTO o : ordenes) {
            lista.append("🆔 Orden #").append(o.getOrdenId())
                    .append(" — Trámite #").append(o.getTramiteId())
                    .append(" — $").append(o.getValor())
                    .append("\n");
        }
        lista.append("\n👉 Escribe *aprobar orden ID* para aprobar una orden.");
        return lista.toString();
    }

    private String aprobarOrden(Long ordenId, SesionUsuario sesion) {
        Optional<OrdenTransferenciaResponseDTO> ordenOpt = ordenTransferenciaService.buscarPorId(ordenId);
        if (ordenOpt.isEmpty()) {
            return "⚠️ No encontré la orden #" + ordenId + ".";
        }

        OrdenTransferenciaResponseDTO orden = ordenOpt.get();
        if (!"Generada".equals(orden.getEstado())) {
            return "⚠️ La orden no está pendiente de aprobación.";
        }

        OrdenTransferenciaRequestDTO ordenDTO = new OrdenTransferenciaRequestDTO();
        ordenDTO.setTramiteId(orden.getTramiteId());
        ordenDTO.setColectoraId(orden.getColectoraId());
        ordenDTO.setDirectorId(sesion.getUsuarioId());
        ordenDTO.setNumeroCuenta(orden.getNumeroCuenta());
        ordenDTO.setValor(orden.getValor());
        ordenDTO.setConcepto(orden.getConcepto());
        ordenDTO.setFechaGeneracion(orden.getFechaGeneracion());
        ordenDTO.setFechaAprobacion(LocalDate.now());
        ordenDTO.setEstado("Aprobada");

        ordenTransferenciaService.actualizar(ordenId, ordenDTO);

        tramiteService.actualizarEstado(orden.getTramiteId(), "Orden aprobada");

        HistorialEstadoRequestDTO historialDTO = new HistorialEstadoRequestDTO();
        historialDTO.setTramiteId(orden.getTramiteId());
        historialDTO.setEstadoAnterior("Orden generada");
        historialDTO.setEstadoNuevo("Orden aprobada");
        historialDTO.setUsuarioId(sesion.getUsuarioId());
        historialDTO.setObservacion("Orden de transferencia #" + ordenId + " aprobada por el Director Financiero");
        historialEstadoService.guardar(historialDTO);

        // ✅ Notificar a COLECTORA y CONTADORA
        String correoColectora = usuarioService.obtenerCorreoPorRol("Colectora");
        if (correoColectora != null) {
            NotificacionRequestDTO notifColectora = new NotificacionRequestDTO();
            notifColectora.setTramiteId(orden.getTramiteId());
            notifColectora.setDestinatario(correoColectora);
            notifColectora.setTipo("Orden de transferencia aprobada");
            notificacionService.guardar(notifColectora);
        }

        String correoContadora = usuarioService.obtenerCorreoPorRol("Contadora");
        if (correoContadora != null) {
            NotificacionRequestDTO notifContadora = new NotificacionRequestDTO();
            notifContadora.setTramiteId(orden.getTramiteId());
            notifContadora.setDestinatario(correoContadora);
            notifContadora.setTipo("Orden aprobada - Registrar comprobante");
            notificacionService.guardar(notifContadora);
        }

        return "✅ Orden #" + ordenId + " aprobada correctamente.\n\n" +
                "📬 Se ha notificado a la *Colectora* y a la *Contadora*.\n\n" +
                "📌 La *Contadora* debe registrar el comprobante con:\n" +
                "*registrar comprobante " + orden.getTramiteId() + "*";
    }

    // ============================================================
    // REEMBOLSOS (solo Director Financiero)
    // ============================================================
    private String listarReembolsosPendientes() {
        List<ReembolsoResponseDTO> reembolsos = reembolsoService.listarPorEstado("Pendiente");

        if (reembolsos.isEmpty()) {
            return "📋 No hay reembolsos pendientes de autorización.\n\n" +
                    "👉 Escribe *menu* para volver al menú principal.";
        }

        StringBuilder lista = new StringBuilder("📋 *Reembolsos pendientes de autorización:* (" + reembolsos.size() + ")\n\n");
        for (ReembolsoResponseDTO r : reembolsos) {
            lista.append("🆔 Reembolso #").append(r.getReembolsoId())
                    .append(" — Trámite #").append(r.getTramiteId())
                    .append(" — $").append(r.getMontoSolicitado())
                    .append("\n");
        }
        lista.append("\n👉 Escribe *autorizar reembolso ID* para autorizar un reembolso.");
        return lista.toString();
    }

    private String autorizarReembolso(Long reembolsoId, SesionUsuario sesion) {
        try {
            ReembolsoResponseDTO reembolso = reembolsoService.autorizar(
                    reembolsoId, sesion.getUsuarioId(), "Autorizado por el Director Financiero");

            Long tramiteId = reembolso.getTramiteId();

            // Notificar a la Contadora para que informe a la Colectora
            String correoContadora = usuarioService.obtenerCorreoPorRol("Contadora");
            if (correoContadora != null) {
                NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
                notifDTO.setTramiteId(tramiteId);
                notifDTO.setDestinatario(correoContadora);
                notifDTO.setTipo("Reembolso autorizado - Informa a Colectora");
                notificacionService.guardar(notifDTO);
            }

            // Historial
            HistorialEstadoRequestDTO historialDTO = new HistorialEstadoRequestDTO();
            historialDTO.setTramiteId(tramiteId);
            historialDTO.setEstadoAnterior("Liquidación aprobada");
            historialDTO.setEstadoNuevo("Reembolso autorizado");
            historialDTO.setUsuarioId(sesion.getUsuarioId());
            historialDTO.setObservacion("Reembolso #" + reembolsoId + " autorizado por el Director Financiero");
            historialEstadoService.guardar(historialDTO);

            return "✅ *Reembolso #" + reembolsoId + " autorizado.*\n\n" +
                    "💰 Monto: $" + reembolso.getMontoSolicitado() + "\n" +
                    "🆔 Trámite: #" + tramiteId + "\n\n" +
                    "📬 Se ha notificado a la *Contadora* para que informe a la Colectora.\n\n" +
                    "📌 La Contadora debe escribir *informar colectora " + tramiteId + "*.";
        } catch (Exception e) {
            return "⚠️ No se pudo autorizar el reembolso.\n\n" +
                    "📌 Detalle: " + e.getMessage();
        }
    }
}