package ec.edu.iti.anticipos.chatbot.service.chat;

import ec.edu.iti.anticipos.chatbot.dto.request.DevolucionRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.request.HistorialEstadoRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.request.NotificacionRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.LiquidacionResponseDTO;
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
 * 1. Aprobar solicitudes (después del Director).
 * 2. Aprobar liquidaciones (después del Asistente Contable).
 */
@Service
@RequiredArgsConstructor
public class ContadoraChatService {

    private final TramiteService tramiteService;
    private final NotificacionService notificacionService;
    private final HistorialEstadoService historialEstadoService;
    private final LiquidacionService liquidacionService;
    private final DevolucionService devolucionService;

    public String procesar(String mensaje, String mensajeLower, SesionUsuario sesion) {

        // Si está esperando motivo de rechazo
        if (sesion.isEsperandoMotivoRechazo()) {
            return procesarMotivoRechazo(mensaje, sesion);
        }

        // Ver solicitudes pendientes (aprobadas por Director)
        if (mensajeLower.contains("solicitud") && mensajeLower.contains("pendiente")) {
            return listarSolicitudesPendientes();
        }

        // Ver liquidaciones pendientes (revisadas por Asistente)
        if (mensajeLower.contains("liquidacion") && mensajeLower.contains("pendiente")) {
            return listarLiquidacionesPendientes();
        }

        // Menú general: mostrar ambos
        if (mensajeLower.contains("pendiente") || mensajeLower.contains("revisar")) {
            return listarTodo();
        }

        // Seleccionar un trámite
        if (mensajeLower.matches("seleccionar\\s+\\d+")) {
            return seleccionarTramite(mensaje, sesion);
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
            return "No hay solicitudes pendientes de revisión.";
        }

        StringBuilder lista = new StringBuilder("Solicitudes pendientes:\n");
        for (TramiteResponseDTO t : pendientes) {
            lista.append("- Trámite ID: ").append(t.getTramiteId())
                    .append(" | Solicitante: ").append(t.getNombreUsuario())
                    .append("\n");
        }
        lista.append("\nEscribe 'seleccionar ID' para revisar una solicitud.");
        return lista.toString();
    }

    // Lista liquidaciones en estado "Liquidación revisada"
    private String listarLiquidacionesPendientes() {
        List<TramiteResponseDTO> pendientes = tramiteService.listarPorEstado("Liquidación revisada");

        if (pendientes.isEmpty()) {
            return "No hay liquidaciones pendientes de aprobación.";
        }

        StringBuilder lista = new StringBuilder("Liquidaciones pendientes:\n");
        for (TramiteResponseDTO t : pendientes) {
            lista.append("- Trámite ID: ").append(t.getTramiteId())
                    .append(" | Docente: ").append(t.getNombreUsuario())
                    .append("\n");
        }
        lista.append("\nEscribe 'seleccionar ID' para revisar una liquidación.");
        return lista.toString();
    }

    // Lista ambos tipos
    private String listarTodo() {
        StringBuilder lista = new StringBuilder();

        List<TramiteResponseDTO> solicitudes = tramiteService.listarPorEstado("Aprobado por Director");
        List<TramiteResponseDTO> liquidaciones = tramiteService.listarPorEstado("Liquidación revisada");

        if (solicitudes.isEmpty() && liquidaciones.isEmpty()) {
            return "No hay trámites pendientes de revisión.";
        }

        if (!solicitudes.isEmpty()) {
            lista.append("Solicitudes pendientes:\n");
            for (TramiteResponseDTO t : solicitudes) {
                lista.append("- Trámite ID: ").append(t.getTramiteId())
                        .append(" | Solicitante: ").append(t.getNombreUsuario())
                        .append("\n");
            }
        }

        if (!liquidaciones.isEmpty()) {
            lista.append("\nLiquidaciones pendientes:\n");
            for (TramiteResponseDTO t : liquidaciones) {
                lista.append("- Trámite ID: ").append(t.getTramiteId())
                        .append(" | Docente: ").append(t.getNombreUsuario())
                        .append("\n");
            }
        }

        lista.append("\nEscribe 'seleccionar ID' para revisar un trámite.");
        return lista.toString();
    }

    // Selecciona un trámite para revisar
    private String seleccionarTramite(String mensaje, SesionUsuario sesion) {
        try {
            Long tramiteId = Long.parseLong(mensaje.replaceAll("[^0-9]", ""));
            sesion.setTramiteEnRevision(tramiteId);
            return "Trámite ID: " + tramiteId + " seleccionado.\n" +
                    "Escribe 'aprobar' o 'rechazar'.";
        } catch (NumberFormatException e) {
            return "Formato incorrecto. Escribe 'seleccionar ID'.";
        }
    }

    // Aprueba un trámite (detecta si es solicitud o liquidación)
    private String aprobar(SesionUsuario sesion) {
        if (sesion.getTramiteEnRevision() == null) {
            return "Primero debes seleccionar un trámite con 'seleccionar ID'.";
        }

        Long tramiteId = sesion.getTramiteEnRevision();
        String estadoActual = tramiteService.buscarPorId(tramiteId)
                .map(TramiteResponseDTO::getEstado)
                .orElse("");

        if (estadoActual.equals("Aprobado por Director")) {
            // Es una solicitud
            return aprobarSolicitud(tramiteId, sesion);
        } else if (estadoActual.equals("Liquidación revisada")) {
            // Es una liquidación
            return aprobarLiquidacion(tramiteId, sesion);
        } else {
            return "El trámite " + tramiteId + " no está pendiente de aprobación.";
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
        return "Solicitud " + tramiteId + " aprobada. Se ha notificado a la Colectora.";
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

        // Verificar si hay sobrante
        Optional<LiquidacionResponseDTO> liquidacionOpt = liquidacionService.buscarPorTramite(tramiteId);

        if (liquidacionOpt.isPresent()) {
            LiquidacionResponseDTO liquidacion = liquidacionOpt.get();

            if (liquidacion.getSaldo() != null && liquidacion.getSaldo().compareTo(BigDecimal.ZERO) > 0) {
                // Registrar devolución pendiente
                DevolucionRequestDTO devolucionDTO = new DevolucionRequestDTO();
                devolucionDTO.setLiquidacionId(liquidacion.getLiquidacionId());
                devolucionDTO.setMonto(liquidacion.getSaldo());
                devolucionService.guardar(devolucionDTO);

                // Notificar al docente
                NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
                notifDTO.setTramiteId(tramiteId);
                notifDTO.setDestinatario("juan.perez@iti.edu.ec");
                notifDTO.setTipo("Devolución pendiente");
                notificacionService.guardar(notifDTO);

                sesion.setTramiteEnRevision(null);

                return "Liquidación " + tramiteId + " aprobada. El docente debe devolver $" +
                        liquidacion.getSaldo() + " al ITI. Se ha notificado al docente.";
            }
        }

        // Si no hay sobrante
        NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
        notifDTO.setTramiteId(tramiteId);
        notifDTO.setDestinatario("juan.perez@iti.edu.ec");
        notifDTO.setTipo("Liquidación aprobada");
        notificacionService.guardar(notifDTO);

        sesion.setTramiteEnRevision(null);
        return "Liquidación " + tramiteId + " aprobada. Se ha notificado al docente.";
    }

    // Inicia el rechazo (pide motivo)
    private String iniciarRechazo(SesionUsuario sesion) {
        if (sesion.getTramiteEnRevision() == null) {
            return "Primero debes seleccionar un trámite con 'seleccionar ID'.";
        }
        sesion.setEsperandoMotivoRechazo(true);
        return "¿Cuál es el motivo del rechazo?";
    }

    // Procesa el motivo del rechazo
    private String procesarMotivoRechazo(String motivo, SesionUsuario sesion) {
        sesion.setMotivoRechazo(motivo);
        sesion.setEsperandoMotivoRechazo(false);

        Long tramiteId = sesion.getTramiteEnRevision();
        String estadoActual = tramiteService.buscarPorId(tramiteId)
                .map(TramiteResponseDTO::getEstado)
                .orElse("");

        String estadoAnterior = estadoActual;
        tramiteService.actualizarEstado(tramiteId, "Rechazado");

        HistorialEstadoRequestDTO historialDTO = new HistorialEstadoRequestDTO();
        historialDTO.setTramiteId(tramiteId);
        historialDTO.setEstadoAnterior(estadoAnterior);
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

        return "Trámite " + tramiteId + " rechazado. Motivo: " + motivo;
    }
}
