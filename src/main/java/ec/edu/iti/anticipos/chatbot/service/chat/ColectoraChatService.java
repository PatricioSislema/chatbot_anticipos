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
 * Servicio especializado en la lógica de la Colectora.
 * Registra las transferencias bancarias y notifica al docente y a la contadora.
 */
@Service
@RequiredArgsConstructor
public class ColectoraChatService {

    private final TramiteService tramiteService;
    private final NotificacionService notificacionService;
    private final HistorialEstadoService historialEstadoService;

    public String procesar(String mensaje, String mensajeLower, SesionUsuario sesion) {

        // Ver trámites aprobados por la Contadora
        if (mensajeLower.contains("pendiente") || mensajeLower.contains("revisar")) {
            return listarPendientes();
        }

        // Registrar un trámite específico
        if (mensajeLower.matches("registrar\\s+\\d+")) {
            return seleccionarTramite(mensaje, sesion);
        }

        // Confirmar transferencia
        if (mensajeLower.contains("transferencia") || mensajeLower.contains("confirmar")) {
            return registrarTransferencia(sesion);
        }

        return null;
    }

    // Lista trámites en estado "Aprobado por Contadora"
    private String listarPendientes() {
        List<TramiteResponseDTO> pendientes = tramiteService.listarPorEstado("Aprobado por Contadora");

        if (pendientes.isEmpty()) {
            return "No hay transferencias pendientes.";
        }

        StringBuilder lista = new StringBuilder("Transferencias pendientes:\n");
        for (TramiteResponseDTO t : pendientes) {
            lista.append("- Trámite ID: ").append(t.getTramiteId())
                    .append(" | Solicitante: ").append(t.getNombreUsuario())
                    .append("\n");
        }
        lista.append("\nEscribe 'registrar ID' para registrar la transferencia.");
        return lista.toString();
    }

    // Selecciona un trámite para registrar transferencia
    private String seleccionarTramite(String mensaje, SesionUsuario sesion) {
        try {
            Long tramiteId = Long.parseLong(mensaje.replaceAll("[^0-9]", ""));
            sesion.setTramiteEnRevision(tramiteId);
            return "Trámite ID: " + tramiteId + " seleccionado.\n" +
                    "Escribe 'confirmar transferencia' para registrar.";
        } catch (NumberFormatException e) {
            return "Formato incorrecto. Escribe 'registrar ID' (ej: registrar 3).";
        }
    }

    // Registra la transferencia y cambia el estado
    private String registrarTransferencia(SesionUsuario sesion) {
        if (sesion.getTramiteEnRevision() == null) {
            return "Primero debes seleccionar un trámite con 'registrar ID'.";
        }

        Long tramiteId = sesion.getTramiteEnRevision();
        tramiteService.actualizarEstado(tramiteId, "Transferido");

        // Historial
        HistorialEstadoRequestDTO historialDTO = new HistorialEstadoRequestDTO();
        historialDTO.setTramiteId(tramiteId);
        historialDTO.setEstadoAnterior("Aprobado por Contadora");
        historialDTO.setEstadoNuevo("Transferido");
        historialDTO.setUsuarioId(sesion.getUsuarioId());
        historialDTO.setObservacion("Transferencia registrada por la Colectora");
        historialEstadoService.guardar(historialDTO);

        // Notificar al docente
        NotificacionRequestDTO notifDocente = new NotificacionRequestDTO();
        notifDocente.setTramiteId(tramiteId);
        notifDocente.setDestinatario("juan.perez@iti.edu.ec");
        notifDocente.setTipo("Anticipo transferido");
        notificacionService.guardar(notifDocente);

        // Notificar a la contadora
        NotificacionRequestDTO notifContadora = new NotificacionRequestDTO();
        notifContadora.setTramiteId(tramiteId);
        notifContadora.setDestinatario("ana.torres@iti.edu.ec");
        notifContadora.setTipo("Transferencia registrada");
        notificacionService.guardar(notifContadora);

        sesion.setTramiteEnRevision(null);

        return "Transferencia registrada. El anticipo ha sido transferido al docente.";
    }
}
