package ec.edu.iti.anticipos.chatbot.service.chat;

import ec.edu.iti.anticipos.chatbot.dto.request.HistorialEstadoRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.request.NotificacionRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.TramiteDetalleDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.TramiteResponseDTO;
import ec.edu.iti.anticipos.chatbot.service.HistorialEstadoService;
import ec.edu.iti.anticipos.chatbot.service.NotificacionService;
import ec.edu.iti.anticipos.chatbot.service.TramiteService;
import ec.edu.iti.anticipos.chatbot.sesion.SesionUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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

        // Aceptar "registrar ID"
        if (mensajeLower.matches("registrar\\s+\\d+")) {
            return seleccionarTramite(mensaje, sesion);
        }

        // Aceptar solo el número
        if (mensajeLower.matches("\\d+")) {
            return seleccionarTramitePorId(Long.parseLong(mensajeLower), sesion);
        }

        if (mensajeLower.contains("pendiente") || mensajeLower.contains("revisar")) {
            return listarPendientes();
        }

        if (mensajeLower.contains("transferencia") || mensajeLower.contains("confirmar")) {
            return registrarTransferencia(sesion);
        }

        return null;
    }

    private String listarPendientes() {
        List<TramiteResponseDTO> pendientes = tramiteService.listarPorEstado("Aprobado por Contadora");

        if (pendientes.isEmpty()) {
            return "📋 No hay transferencias pendientes.\n\n" +
                    "👉 Escribe *menu* para volver al menú principal.";
        }

        StringBuilder lista = new StringBuilder("📋 *Transferencias pendientes:* (" + pendientes.size() + ")\n\n");
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
        lista.append("\n👉 Escribe *registrar ID* o solo el número.");
        return lista.toString();
    }

    private String seleccionarTramite(String mensaje, SesionUsuario sesion) {
        try {
            Long tramiteId = Long.parseLong(mensaje.replaceAll("[^0-9]", ""));
            return seleccionarTramitePorId(tramiteId, sesion);
        } catch (NumberFormatException e) {
            return "⚠️ Formato incorrecto.";
        }
    }

    private String seleccionarTramitePorId(Long tramiteId, SesionUsuario sesion) {
        sesion.setTramiteEnRevision(tramiteId);

        Optional<TramiteDetalleDTO> detalleOpt = tramiteService.obtenerDetalle(tramiteId);
        if (detalleOpt.isEmpty()) {
            return "⚠️ No encontré el trámite *" + tramiteId + "*.";
        }

        TramiteDetalleDTO d = detalleOpt.get();

        StringBuilder sb = new StringBuilder();
        sb.append("📋 *Trámite #").append(d.getTramiteId()).append("*\n\n");
        sb.append("👤 Solicitante: ").append(d.getNombreUsuario()).append("\n");
        sb.append("📝 Tipo: ").append(d.getNombreTipoTramite()).append("\n");
        sb.append("📊 Estado: ").append(d.getEstado()).append("\n");

        if (d.getPresupuesto() != null) {
            sb.append("\n💰 Presupuesto: $").append(d.getPresupuesto()).append("\n");
            if (d.getProposito() != null) sb.append("🎯 Propósito: ").append(d.getProposito()).append("\n");
        }

        sb.append("\n¿Registrar la transferencia?\n\n");
        sb.append("Escribe *confirmar transferencia*.");
        return sb.toString();
    }

    private String registrarTransferencia(SesionUsuario sesion) {
        if (sesion.getTramiteEnRevision() == null) {
            return "⚠️ No has seleccionado ningún trámite.";
        }

        Long tramiteId = sesion.getTramiteEnRevision();
        tramiteService.actualizarEstado(tramiteId, "Transferido");

        HistorialEstadoRequestDTO historialDTO = new HistorialEstadoRequestDTO();
        historialDTO.setTramiteId(tramiteId);
        historialDTO.setEstadoAnterior("Aprobado por Contadora");
        historialDTO.setEstadoNuevo("Transferido");
        historialDTO.setUsuarioId(sesion.getUsuarioId());
        historialDTO.setObservacion("Transferencia registrada por la Colectora");
        historialEstadoService.guardar(historialDTO);

        NotificacionRequestDTO notifDocente = new NotificacionRequestDTO();
        notifDocente.setTramiteId(tramiteId);
        notifDocente.setDestinatario("juan.perez@iti.edu.ec");
        notifDocente.setTipo("Anticipo transferido");
        notificacionService.guardar(notifDocente);

        NotificacionRequestDTO notifContadora = new NotificacionRequestDTO();
        notifContadora.setTramiteId(tramiteId);
        notifContadora.setDestinatario("ana.torres@iti.edu.ec");
        notifContadora.setTipo("Transferencia registrada");
        notificacionService.guardar(notifContadora);

        sesion.setTramiteEnRevision(null);

        return "✅ Transferencia registrada correctamente.\n\n" +
                "💰 El anticipo ha sido transferido al docente.\n\n" +
                "📬 Se ha notificado al *Docente* y a la *Contadora*.";
    }
}