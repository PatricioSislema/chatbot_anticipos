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
 * Servicio común para los roles aprobadores: Jefe, Director y Contadora.
 * La lógica es idéntica, solo cambia la configuración (estados, destinatarios).
 */
@Service
@RequiredArgsConstructor
public class AprobadorChatService {

    private final TramiteService tramiteService;
    private final NotificacionService notificacionService;
    private final HistorialEstadoService historialEstadoService;

    // Procesa los mensajes del aprobador
    public String procesar(String mensaje, String mensajeLower, SesionUsuario sesion,
                           ConfiguracionAprobador config) {

        // Si está esperando el motivo del rechazo
        if (sesion.isEsperandoMotivoRechazo()) {
            return procesarMotivoRechazo(mensaje, sesion, config);
        }

        // El aprobador quiere revisar un trámite específico (con número)
        if (mensajeLower.matches("revisar\\s+\\d+")) {
            return seleccionarTramite(mensaje, sesion);
        }

        // El aprobador pide ver trámites pendientes
        if (mensajeLower.contains("pendiente") || mensajeLower.contains("revisar")) {
            return listarPendientes(sesion, config);
        }

        // El aprobador aprueba
        if (mensajeLower.contains("aprobar")) {
            return aprobar(sesion, config);
        }

        // El aprobador rechaza
        if (mensajeLower.contains("rechazar")) {
            return iniciarRechazo(sesion);
        }

        return null;
    }

    // Lista los trámites pendientes según el estado configurado
    private String listarPendientes(SesionUsuario sesion, ConfiguracionAprobador config) {
        List<TramiteResponseDTO> pendientes = tramiteService.listarPorEstado(config.getEstadoBuscado());

        if (pendientes.isEmpty()) {
            return "No hay trámites pendientes de revisión.";
        }

        StringBuilder lista = new StringBuilder("Trámites pendientes:\n");
        for (TramiteResponseDTO t : pendientes) {
            lista.append("- Trámite ID: ").append(t.getTramiteId())
                    .append(" | Solicitante: ").append(t.getNombreUsuario())
                    .append(" | Estado: ").append(t.getEstado())
                    .append("\n");
        }
        lista.append("\nEscribe 'revisar ID' para ver el detalle de un trámite.");
        return lista.toString();
    }

    // Selecciona un trámite para revisar
    private String seleccionarTramite(String mensaje, SesionUsuario sesion) {
        try {
            Long tramiteId = Long.parseLong(mensaje.replaceAll("[^0-9]", ""));
            sesion.setTramiteEnRevision(tramiteId);
            return "Revisando trámite ID: " + tramiteId + ".\n" +
                    "Puedes escribir 'aprobar' o 'rechazar'.";
        } catch (NumberFormatException e) {
            return "Formato incorrecto. Escribe 'revisar ID' (ej: revisar 3).";
        }
    }

    // Aprueba el trámite
    private String aprobar(SesionUsuario sesion, ConfiguracionAprobador config) {
        if (sesion.getTramiteEnRevision() == null) {
            return "Primero debes seleccionar un trámite con 'revisar ID'.";
        }

        Long tramiteId = sesion.getTramiteEnRevision();
        String estadoAnterior = config.getEstadoBuscado();
        String nuevoEstado = config.getNuevoEstado();

        // Actualizar estado del trámite
        tramiteService.actualizarEstado(tramiteId, nuevoEstado);

        // Registrar historial
        HistorialEstadoRequestDTO historialDTO = new HistorialEstadoRequestDTO();
        historialDTO.setTramiteId(tramiteId);
        historialDTO.setEstadoAnterior(estadoAnterior);
        historialDTO.setEstadoNuevo(nuevoEstado);
        historialDTO.setUsuarioId(sesion.getUsuarioId());
        historialDTO.setObservacion("Aprobado por " + config.getRolNombre());
        historialEstadoService.guardar(historialDTO);

        // Notificar al siguiente actor
        NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
        notifDTO.setTramiteId(tramiteId);
        notifDTO.setDestinatario(config.getDestinatarioAprobacion());
        notifDTO.setTipo("Trámite aprobado por " + config.getRolNombre());
        notificacionService.guardar(notifDTO);

        sesion.setTramiteEnRevision(null);

        return "Trámite " + tramiteId + " aprobado. Se ha notificado al siguiente responsable.";
    }

    // Inicia el flujo de rechazo (pide motivo)
    private String iniciarRechazo(SesionUsuario sesion) {
        if (sesion.getTramiteEnRevision() == null) {
            return "Primero debes seleccionar un trámite con 'revisar ID'.";
        }
        sesion.setEsperandoMotivoRechazo(true);
        return "¿Cuál es el motivo del rechazo?";
    }

    // Procesa el motivo del rechazo y rechaza el trámite
    private String procesarMotivoRechazo(String motivo, SesionUsuario sesion,
                                         ConfiguracionAprobador config) {
        sesion.setMotivoRechazo(motivo);
        sesion.setEsperandoMotivoRechazo(false);

        Long tramiteId = sesion.getTramiteEnRevision();
        String estadoAnterior = config.getEstadoBuscado();

        // Actualizar estado del trámite
        tramiteService.actualizarEstado(tramiteId, "Rechazado");

        // Registrar historial
        HistorialEstadoRequestDTO historialDTO = new HistorialEstadoRequestDTO();
        historialDTO.setTramiteId(tramiteId);
        historialDTO.setEstadoAnterior(estadoAnterior);
        historialDTO.setEstadoNuevo("Rechazado");
        historialDTO.setUsuarioId(sesion.getUsuarioId());
        historialDTO.setObservacion("Rechazado por " + config.getRolNombre() + ": " + motivo);
        historialEstadoService.guardar(historialDTO);

        // Notificar al docente
        NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
        notifDTO.setTramiteId(tramiteId);
        notifDTO.setDestinatario(config.getDestinatarioRechazo());
        notifDTO.setTipo("Trámite rechazado por " + config.getRolNombre());
        notificacionService.guardar(notifDTO);

        // Limpiar sesión
        sesion.setTramiteEnRevision(null);
        sesion.setMotivoRechazo(null);

        return "Trámite " + tramiteId + " rechazado. Motivo: " + motivo +
                ". Se ha notificado al docente.";
    }
}
