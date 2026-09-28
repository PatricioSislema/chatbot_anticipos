package ec.edu.iti.anticipos.chatbot.service.chat;

import ec.edu.iti.anticipos.chatbot.dto.request.HistorialEstadoRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.request.NotificacionRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.TramiteResponseDTO;
import ec.edu.iti.anticipos.chatbot.service.HistorialEstadoService;
import ec.edu.iti.anticipos.chatbot.service.NotificacionService;
import ec.edu.iti.anticipos.chatbot.service.TramiteService;
import ec.edu.iti.anticipos.chatbot.sesion.SesionUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio especializado en la lógica del Asistente Contable.
 * Revisa las liquidaciones enviadas por el docente y las deriva a la Contadora
 * o las devuelve al docente con observaciones.
 */
@Service
@RequiredArgsConstructor
public class AsistenteContableChatService {

    private final TramiteService tramiteService;
    private final NotificacionService notificacionService;
    private final HistorialEstadoService historialEstadoService;

    public String procesar(String mensaje, String mensajeLower, SesionUsuario sesion) {

        // Si está esperando observaciones para devolver al docente
        if (sesion.isEsperandoMotivoRechazo()) {
            return devolverAlDocente(mensaje, sesion);
        }

        // Ver liquidaciones pendientes
        if (mensajeLower.contains("pendiente") || mensajeLower.contains("revisar")) {
            return listarPendientes();
        }

        // Seleccionar una liquidación específica
        if (mensajeLower.matches("seleccionar\\s+\\d+")) {
            return seleccionarLiquidacion(mensaje, sesion);
        }

        // Enviar a la Contadora
        if (mensajeLower.contains("enviar") || mensajeLower.contains("aprobar")) {
            return enviarAContadora(sesion);
        }

        // Devolver al docente
        if (mensajeLower.contains("devolver") || mensajeLower.contains("rechazar")) {
            return iniciarDevolucion(sesion);
        }

        return null;
    }

    // Lista liquidaciones en estado "Liquidación en proceso"
    private String listarPendientes() {
        List<TramiteResponseDTO> pendientes = tramiteService.listarPorEstado("Liquidación en proceso");

        if (pendientes.isEmpty()) {
            return "No hay liquidaciones pendientes de revisión.";
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

    // Selecciona una liquidación para revisar
    private String seleccionarLiquidacion(String mensaje, SesionUsuario sesion) {
        try {
            Long tramiteId = Long.parseLong(mensaje.replaceAll("[^0-9]", ""));
            sesion.setTramiteEnRevision(tramiteId);
            return "Liquidación ID: " + tramiteId + " seleccionada.\n" +
                    "Escribe 'enviar' para derivar a la Contadora o 'devolver' para regresar al docente.";
        } catch (NumberFormatException e) {
            return "Formato incorrecto. Escribe 'seleccionar ID' (ej: seleccionar 6).";
        }
    }

    // Envía la liquidación a la Contadora
    private String enviarAContadora(SesionUsuario sesion) {
        if (sesion.getTramiteEnRevision() == null) {
            return "Primero debes seleccionar una liquidación con 'seleccionar ID'.";
        }

        Long tramiteId = sesion.getTramiteEnRevision();

        // Cambiar estado
        tramiteService.actualizarEstado(tramiteId, "Liquidación revisada");

        // Historial
        HistorialEstadoRequestDTO historialDTO = new HistorialEstadoRequestDTO();
        historialDTO.setTramiteId(tramiteId);
        historialDTO.setEstadoAnterior("Liquidación en proceso");
        historialDTO.setEstadoNuevo("Liquidación revisada");
        historialDTO.setUsuarioId(sesion.getUsuarioId());
        historialDTO.setObservacion("Revisado por el Asistente Contable");
        historialEstadoService.guardar(historialDTO);

        // Notificar a la Contadora
        NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
        notifDTO.setTramiteId(tramiteId);
        notifDTO.setDestinatario("ana.torres@iti.edu.ec");
        notifDTO.setTipo("Liquidación pendiente de aprobación");
        notificacionService.guardar(notifDTO);

        sesion.setTramiteEnRevision(null);

        return "Liquidación enviada a la Contadora para aprobación.";
    }

    // Inicia la devolución al docente (pide observaciones)
    private String iniciarDevolucion(SesionUsuario sesion) {
        if (sesion.getTramiteEnRevision() == null) {
            return "Primero debes seleccionar una liquidación con 'seleccionar ID'.";
        }
        sesion.setEsperandoMotivoRechazo(true);
        return "¿Cuál es la observación para devolver la liquidación al docente?";
    }

    // Procesa las observaciones y devuelve al docente
    private String devolverAlDocente(String observacion, SesionUsuario sesion) {
        sesion.setMotivoRechazo(observacion);
        sesion.setEsperandoMotivoRechazo(false);

        Long tramiteId = sesion.getTramiteEnRevision();

        // Cambiar estado
        tramiteService.actualizarEstado(tramiteId, "Devuelto al docente");

        // Historial
        HistorialEstadoRequestDTO historialDTO = new HistorialEstadoRequestDTO();
        historialDTO.setTramiteId(tramiteId);
        historialDTO.setEstadoAnterior("Liquidación en proceso");
        historialDTO.setEstadoNuevo("Devuelto al docente");
        historialDTO.setUsuarioId(sesion.getUsuarioId());
        historialDTO.setObservacion("Devuelto por el Asistente Contable: " + observacion);
        historialEstadoService.guardar(historialDTO);

        // Notificar al docente
        NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
        notifDTO.setTramiteId(tramiteId);
        notifDTO.setDestinatario("juan.perez@iti.edu.ec");
        notifDTO.setTipo("Liquidación devuelta para corrección");
        notificacionService.guardar(notifDTO);

        sesion.setTramiteEnRevision(null);
        sesion.setMotivoRechazo(null);

        return "Liquidación devuelta al docente. Observación: " + observacion;
    }
}
