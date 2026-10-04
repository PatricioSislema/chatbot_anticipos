package ec.edu.iti.anticipos.chatbot.service.chat;

import ec.edu.iti.anticipos.chatbot.dto.request.DevolucionRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.request.HistorialEstadoRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.request.NotificacionRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.LiquidacionResponseDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.TramiteDetalleDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.TramiteResponseDTO;
import ec.edu.iti.anticipos.chatbot.service.DevolucionService;
import ec.edu.iti.anticipos.chatbot.service.HistorialEstadoService;
import ec.edu.iti.anticipos.chatbot.service.LiquidacionService;
import ec.edu.iti.anticipos.chatbot.service.NotificacionService;
import ec.edu.iti.anticipos.chatbot.service.TramiteService;
import ec.edu.iti.anticipos.chatbot.sesion.SesionUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Servicio especializado en la lógica de la Contadora.
 * Maneja dos flujos:
 *  1. Aprobar solicitudes (después del Director).
 *  2. Aprobar liquidaciones (después del Asistente Contable).
 */
@Service
@RequiredArgsConstructor
public class ContadoraChatService {

    private final TramiteService tramiteService;
    private final NotificacionService notificacionService;
    private final HistorialEstadoService historialEstadoService;
    private final LiquidacionService liquidacionService;
    private final DevolucionService devolucionService;

    // Procesa los mensajes de la Contadora
    public String procesar(String mensaje, String mensajeLower, SesionUsuario sesion) {

        if (sesion.isEsperandoMotivoRechazo()) {
            return procesarMotivoRechazo(mensaje, sesion);
        }

        // Aceptar "seleccionar ID" o solo "ID"
        if (mensajeLower.matches("seleccionar\\s+\\d+")) {
            return seleccionarTramite(mensaje, sesion);
        }

        // Aceptar solo el número (ej. "21")
        if (mensajeLower.matches("\\d+")) {
            return seleccionarTramitePorId(Long.parseLong(mensajeLower), sesion);
        }

        // Ver solicitudes pendientes
        if (mensajeLower.contains("solicitud") && mensajeLower.contains("pendiente")) {
            return listarSolicitudesPendientes();
        }

        // Ver liquidaciones pendientes
        if (mensajeLower.contains("liquidacion") && mensajeLower.contains("pendiente")) {
            return listarLiquidacionesPendientes();
        }

        // Menú general: mostrar ambos
        if (mensajeLower.contains("pendiente") || mensajeLower.contains("revisar")) {
            return listarTodo();
        }

        // Aprobar
        if (mensajeLower.contains("aprobar")) {
            return aprobar(sesion);
        }

        // Rechazar
        if (mensajeLower.contains("rechazar")) {
            return iniciarRechazo(sesion);
        }

        return null;
    }

    // Lista solicitudes en estado "Aprobado por Director"
    private String listarSolicitudesPendientes() {
        List<TramiteResponseDTO> pendientes = tramiteService.listarPorEstado("Aprobado por Director");

        if (pendientes.isEmpty()) {
            return "📋 No hay solicitudes pendientes de revisión.\n\n" +
                    "👉 Escribe *menu* para volver al menú principal.";
        }

        StringBuilder lista = new StringBuilder("📋 *Solicitudes pendientes:* (" + pendientes.size() + ")\n\n");
        for (TramiteResponseDTO t : pendientes) {
            lista.append("🆔 ").append(t.getTramiteId())
                    .append(" — ").append(t.getNombreUsuario());
            Optional<TramiteDetalleDTO> detalle = tramiteService.obtenerDetalle(t.getTramiteId());
            if (detalle.isPresent() && detalle.get().getProposito() != null) {
                String proposito = detalle.get().getProposito();
                if (proposito.length() > 30) proposito = proposito.substring(0, 30) + "...";
                lista.append(" · ").append(proposito);
            }
            lista.append("\n");
        }
        lista.append("\n👉 Escribe *seleccionar ID* o solo el número para revisar.");
        return lista.toString();
    }

    // Lista liquidaciones en estado "Liquidación revisada"
    private String listarLiquidacionesPendientes() {
        List<TramiteResponseDTO> pendientes = tramiteService.listarPorEstado("Liquidación revisada");

        if (pendientes.isEmpty()) {
            return "📋 No hay liquidaciones pendientes de aprobación.\n\n" +
                    "👉 Escribe *menu* para volver al menú principal.";
        }

        StringBuilder lista = new StringBuilder("📋 *Liquidaciones pendientes:* (" + pendientes.size() + ")\n\n");
        for (TramiteResponseDTO t : pendientes) {
            lista.append("🆔 ").append(t.getTramiteId())
                    .append(" — ").append(t.getNombreUsuario()).append("\n");
        }
        lista.append("\n👉 Escribe *seleccionar ID* o solo el número para revisar.");
        return lista.toString();
    }

    // Lista ambos tipos
    private String listarTodo() {
        StringBuilder lista = new StringBuilder();

        List<TramiteResponseDTO> solicitudes = tramiteService.listarPorEstado("Aprobado por Director");
        List<TramiteResponseDTO> liquidaciones = tramiteService.listarPorEstado("Liquidación revisada");

        if (solicitudes.isEmpty() && liquidaciones.isEmpty()) {
            return "📋 No hay trámites pendientes de revisión.\n\n" +
                    "👉 Escribe *menu* para volver al menú principal.";
        }

        if (!solicitudes.isEmpty()) {
            lista.append("📋 *Solicitudes pendientes:* (").append(solicitudes.size()).append(")\n\n");
            for (TramiteResponseDTO t : solicitudes) {
                lista.append("🆔 ").append(t.getTramiteId())
                        .append(" — ").append(t.getNombreUsuario());
                Optional<TramiteDetalleDTO> detalle = tramiteService.obtenerDetalle(t.getTramiteId());
                if (detalle.isPresent() && detalle.get().getProposito() != null) {
                    String proposito = detalle.get().getProposito();
                    if (proposito.length() > 30) proposito = proposito.substring(0, 30) + "...";
                    lista.append(" · ").append(proposito);
                }
                lista.append("\n");
            }
        }

        if (!liquidaciones.isEmpty()) {
            lista.append("\n📋 *Liquidaciones pendientes:* (").append(liquidaciones.size()).append(")\n\n");
            for (TramiteResponseDTO t : liquidaciones) {
                lista.append("🆔 ").append(t.getTramiteId())
                        .append(" — ").append(t.getNombreUsuario()).append("\n");
            }
        }

        lista.append("\n👉 Escribe *seleccionar ID* o solo el número para revisar.");
        return lista.toString();
    }

    // Selecciona un trámite (versión vieja que recibe "seleccionar 15")
    private String seleccionarTramite(String mensaje, SesionUsuario sesion) {
        try {
            Long tramiteId = Long.parseLong(mensaje.replaceAll("[^0-9]", ""));
            return seleccionarTramitePorId(tramiteId, sesion);
        } catch (NumberFormatException e) {
            return "⚠️ Formato incorrecto.\n\n" +
                    "📌 Debes escribir *seleccionar* seguido del número.\n\n" +
                    "👉 Ejemplo: seleccionar 15";
        }
    }

    // Selecciona un trámite por ID
    private String seleccionarTramitePorId(Long tramiteId, SesionUsuario sesion) {
        sesion.setTramiteEnRevision(tramiteId);

        Optional<TramiteDetalleDTO> detalleOpt = tramiteService.obtenerDetalle(tramiteId);
        if (detalleOpt.isEmpty()) {
            return "⚠️ No encontré el trámite *" + tramiteId + "*. Verifica el ID.";
        }

        TramiteDetalleDTO d = detalleOpt.get();

        StringBuilder sb = new StringBuilder();
        sb.append("📋 *Trámite #").append(d.getTramiteId()).append("*\n\n");
        sb.append("👤 Solicitante: ").append(d.getNombreUsuario()).append("\n");
        sb.append("📝 Tipo: ").append(d.getNombreTipoTramite()).append("\n");
        sb.append("📅 Fecha: ").append(d.getFechaCreacion()).append("\n");
        sb.append("📊 Estado: ").append(d.getEstado()).append("\n");

        // Datos de la solicitud (si aplica)
        if (d.getPresupuesto() != null) {
            sb.append("\n💰 Presupuesto: $").append(d.getPresupuesto()).append("\n");
            if (d.getUnidad() != null) sb.append("🏢 Unidad: ").append(d.getUnidad()).append("\n");
            if (d.getCampus() != null) sb.append("📍 Campus: ").append(d.getCampus()).append("\n");
            if (d.getProposito() != null) sb.append("🎯 Propósito: ").append(d.getProposito()).append("\n");
            if (d.getLugar() != null) sb.append("📌 Lugar: ").append(d.getLugar()).append("\n");
            if (d.getFechaRealizacion() != null) sb.append("📅 Fecha realización: ").append(d.getFechaRealizacion()).append("\n");
        }

        // Datos de la liquidación (si aplica)
        if (d.getTotalGastado() != null) {
            sb.append("\n💰 Total gastado: $").append(d.getTotalGastado()).append("\n");
            if (d.getSaldo() != null) sb.append("📈 Saldo: $").append(d.getSaldo()).append("\n");
            if (d.getFechaPresentacion() != null) sb.append("📅 Fecha presentación: ").append(d.getFechaPresentacion()).append("\n");
        }

        sb.append("\n¿Qué quieres hacer?");
        return sb.toString();
    }

    // Aprueba un trámite (detecta si es solicitud o liquidación)
    private String aprobar(SesionUsuario sesion) {
        if (sesion.getTramiteEnRevision() == null) {
            return "⚠️ No has seleccionado ningún trámite.\n\n" +
                    "📌 Primero debes elegir uno de la lista.\n\n" +
                    "👉 Escribe *seleccionar ID* o solo el número.";
        }

        Long tramiteId = sesion.getTramiteEnRevision();
        String estadoActual = tramiteService.buscarPorId(tramiteId)
                .map(TramiteResponseDTO::getEstado)
                .orElse("");

        if (estadoActual.equals("Aprobado por Director")) {
            return aprobarSolicitud(tramiteId, sesion);
        } else if (estadoActual.equals("Liquidación revisada")) {
            return aprobarLiquidacion(tramiteId, sesion);
        } else {
            return "⚠️ El trámite *" + tramiteId + "* no está pendiente de aprobación.";
        }
    }

    // Aprueba una solicitud (envía a Colectora)
    private String aprobarSolicitud(Long tramiteId, SesionUsuario sesion) {
        tramiteService.actualizarEstado(tramiteId, "Aprobado por Contadora");

        HistorialEstadoRequestDTO historialDTO = new HistorialEstadoRequestDTO();
        historialDTO.setTramiteId(tramiteId);
        historialDTO.setEstadoAnterior("Aprobado por Director");
        historialDTO.setEstadoNuevo("Aprobado por Contadora");
        historialDTO.setUsuarioId(sesion.getUsuarioId());
        historialDTO.setObservacion("Aprobado por la Contadora");
        historialEstadoService.guardar(historialDTO);

        NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
        notifDTO.setTramiteId(tramiteId);
        notifDTO.setDestinatario("luis.mora@iti.edu.ec");
        notifDTO.setTipo("Solicitud aprobada por Contadora");
        notificacionService.guardar(notifDTO);

        sesion.setTramiteEnRevision(null);
        return "✅ Solicitud *" + tramiteId + "* aprobada correctamente.\n\n" +
                "📬 Se ha notificado a la *Colectora*.";
    }

    // Aprueba una liquidación (notifica al docente)
    private String aprobarLiquidacion(Long tramiteId, SesionUsuario sesion) {
        tramiteService.actualizarEstado(tramiteId, "Liquidación aprobada");

        HistorialEstadoRequestDTO historialDTO = new HistorialEstadoRequestDTO();
        historialDTO.setTramiteId(tramiteId);
        historialDTO.setEstadoAnterior("Liquidación revisada");
        historialDTO.setEstadoNuevo("Liquidación aprobada");
        historialDTO.setUsuarioId(sesion.getUsuarioId());
        historialDTO.setObservacion("Liquidación aprobada por la Contadora");
        historialEstadoService.guardar(historialDTO);

        Optional<LiquidacionResponseDTO> liquidacionOpt = liquidacionService.buscarPorTramite(tramiteId);

        if (liquidacionOpt.isPresent()) {
            LiquidacionResponseDTO liquidacion = liquidacionOpt.get();

            if (liquidacion.getSaldo() != null && liquidacion.getSaldo().compareTo(BigDecimal.ZERO) > 0) {
                DevolucionRequestDTO devolucionDTO = new DevolucionRequestDTO();
                devolucionDTO.setLiquidacionId(liquidacion.getLiquidacionId());
                devolucionDTO.setMonto(liquidacion.getSaldo());
                devolucionService.guardar(devolucionDTO);

                NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
                notifDTO.setTramiteId(tramiteId);
                notifDTO.setDestinatario("juan.perez@iti.edu.ec");
                notifDTO.setTipo("Devolución pendiente");
                notificacionService.guardar(notifDTO);

                sesion.setTramiteEnRevision(null);
                return "✅ Liquidación *" + tramiteId + "* aprobada correctamente.\n\n" +
                        "💰 El Docente debe devolver $" + liquidacion.getSaldo() + " al ITI.\n\n" +
                        "📬 Se ha notificado al *Docente*.";
            }
        }

        NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
        notifDTO.setTramiteId(tramiteId);
        notifDTO.setDestinatario("juan.perez@iti.edu.ec");
        notifDTO.setTipo("Liquidación aprobada");
        notificacionService.guardar(notifDTO);

        sesion.setTramiteEnRevision(null);
        return "✅ Liquidación *" + tramiteId + "* aprobada correctamente.\n\n" +
                "📬 Se ha notificado al *Docente*.";
    }

    // Inicia el rechazo (pide motivo)
    private String iniciarRechazo(SesionUsuario sesion) {
        if (sesion.getTramiteEnRevision() == null) {
            return "⚠️ No has seleccionado ningún trámite.";
        }
        sesion.setEsperandoMotivoRechazo(true);
        return "✍️ ¿Cuál es el motivo del rechazo?";
    }

    // Procesa el motivo del rechazo
    private String procesarMotivoRechazo(String motivo, SesionUsuario sesion) {
        sesion.setMotivoRechazo(motivo);
        sesion.setEsperandoMotivoRechazo(false);

        Long tramiteId = sesion.getTramiteEnRevision();
        String estadoActual = tramiteService.buscarPorId(tramiteId)
                .map(TramiteResponseDTO::getEstado)
                .orElse("");

        tramiteService.actualizarEstado(tramiteId, "Rechazado");

        HistorialEstadoRequestDTO historialDTO = new HistorialEstadoRequestDTO();
        historialDTO.setTramiteId(tramiteId);
        historialDTO.setEstadoAnterior(estadoActual);
        historialDTO.setEstadoNuevo("Rechazado");
        historialDTO.setUsuarioId(sesion.getUsuarioId());
        historialDTO.setObservacion("Rechazado por Contadora: " + motivo);
        historialEstadoService.guardar(historialDTO);

        NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
        notifDTO.setTramiteId(tramiteId);
        notifDTO.setDestinatario("juan.perez@iti.edu.ec");
        notifDTO.setTipo("Trámite rechazado por Contadora");
        notificacionService.guardar(notifDTO);

        sesion.setTramiteEnRevision(null);
        sesion.setMotivoRechazo(null);

        return "❌ Trámite *" + tramiteId + "* rechazado.\n\n" +
                "📌 Motivo: " + motivo + "\n\n" +
                "📬 Se ha notificado al *Docente*.";
    }
}