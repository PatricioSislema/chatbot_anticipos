package ec.edu.iti.anticipos.chatbot.service.chat;

import ec.edu.iti.anticipos.chatbot.dto.request.HistorialEstadoRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.request.NotificacionRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.TramiteDetalleDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.TramiteResponseDTO;
import ec.edu.iti.anticipos.chatbot.service.HistorialEstadoService;
import ec.edu.iti.anticipos.chatbot.service.NotificacionService;
import ec.edu.iti.anticipos.chatbot.service.TramiteService;
import ec.edu.iti.anticipos.chatbot.service.UsuarioService;
import ec.edu.iti.anticipos.chatbot.sesion.SesionUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Servicio especializado en la lógica del Asistente Contable.
 * Revisa las liquidaciones enviadas por el funcionario y las deriva a la Contadora
 * o las devuelve al funcionario con observaciones.
 */
@Service
@RequiredArgsConstructor
public class AsistenteContableChatService {

    private final TramiteService tramiteService;
    private final NotificacionService notificacionService;
    private final HistorialEstadoService historialEstadoService;
    private final UsuarioService usuarioService;

    public String procesar(String mensaje, String mensajeLower, SesionUsuario sesion) {

        if (sesion.isEsperandoMotivoRechazo()) {
            return devolverAlFuncionario(mensaje, sesion);
        }

        if (mensajeLower.matches("seleccionar\\s+\\d+")) {
            return seleccionarLiquidacion(mensaje, sesion);
        }

        if (mensajeLower.matches("\\d+")) {
            return seleccionarLiquidacionPorId(Long.parseLong(mensajeLower), sesion);
        }

        if (mensajeLower.contains("pendiente") || mensajeLower.contains("revisar")) {
            return listarPendientes();
        }

        if (mensajeLower.contains("enviar") || mensajeLower.contains("aprobar")) {
            return enviarAContadora(sesion);
        }

        if (mensajeLower.contains("devolver") || mensajeLower.contains("rechazar")) {
            return iniciarDevolucion(sesion);
        }

        // ✅ Si el usuario escribe texto inválido cuando está en el menú principal
        if (sesion.getTramiteEnRevision() == null
                && !mensajeLower.isEmpty()
                && !mensajeLower.matches("\\d+")
                && !mensajeLower.contains("menu")
                && !mensajeLower.contains("menú")
                && !mensajeLower.contains("ayuda")
                && !mensajeLower.contains("cancelar")
                && !mensajeLower.contains("salir")
                && !mensajeLower.contains("pendiente")
                && !mensajeLower.contains("revisar")
                && !mensajeLower.contains("enviar")
                && !mensajeLower.contains("aprobar")
                && !mensajeLower.contains("devolver")
                && !mensajeLower.contains("rechazar")) {
            return "⚠️ Por favor, ingresa el *número de la liquidación* que deseas revisar.\n\n" +
                    "👉 Ejemplo: *13*";
        }

        return null;
    }

    private String listarPendientes() {
        List<TramiteResponseDTO> pendientes = tramiteService.listarPorEstado("Liquidación en proceso");

        if (pendientes.isEmpty()) {
            return "📋 No hay liquidaciones pendientes de revisión.\n\n" +
                    "👉 Escribe *menu* para volver al menú principal.";
        }

        StringBuilder lista = new StringBuilder("📋 *Liquidaciones pendientes:* (" + pendientes.size() + ")\n\n");
        for (TramiteResponseDTO t : pendientes) {
            lista.append("🆔 ").append(t.getTramiteId())
                    .append(" — ").append(t.getNombreUsuario()).append("\n");
        }
        lista.append("\n👉 Escribe el *número de la liquidación* que deseas revisar.");
        lista.append("\nEjemplo: *13*.");
        return lista.toString();
    }

    private String seleccionarLiquidacion(String mensaje, SesionUsuario sesion) {
        try {
            Long tramiteId = Long.parseLong(mensaje.replaceAll("[^0-9]", ""));
            return seleccionarLiquidacionPorId(tramiteId, sesion);
        } catch (NumberFormatException e) {
            return "⚠️ Formato incorrecto.";
        }
    }

    private String seleccionarLiquidacionPorId(Long tramiteId, SesionUsuario sesion) {
        sesion.setTramiteEnRevision(tramiteId);

        Optional<TramiteDetalleDTO> detalleOpt = tramiteService.obtenerDetalle(tramiteId);
        if (detalleOpt.isEmpty()) {
            return "⚠️ No encontré la liquidación *" + tramiteId + "*.";
        }

        TramiteDetalleDTO d = detalleOpt.get();

        StringBuilder sb = new StringBuilder();
        sb.append("📋 *Liquidación #").append(d.getTramiteId()).append("*\n\n");
        sb.append("👤 Funcionario: ").append(d.getNombreUsuario()).append("\n");
        sb.append("📝 Tipo: ").append(d.getNombreTipoTramite()).append("\n");
        sb.append("📅 Fecha: ").append(d.getFechaCreacion()).append("\n");
        sb.append("📊 Estado: ").append(d.getEstado()).append("\n");

        if (d.getTotalGastado() != null) {
            sb.append("\n💰 Total gastado: $").append(d.getTotalGastado()).append("\n");
            if (d.getSaldo() != null) {
                sb.append("📈 Saldo: $").append(d.getSaldo()).append("\n");
            }
        }

        if (d.getPresupuesto() != null) {
            sb.append("\n💰 Anticipo original: $").append(d.getPresupuesto()).append("\n");
        }
        if (d.getUnidad() != null) {
            sb.append("🏢 Unidad: ").append(d.getUnidad()).append("\n");
        }
        if (d.getCampus() != null) {
            sb.append("📍 Campus: ").append(d.getCampus()).append("\n");
        }
        if (d.getProposito() != null) {
            sb.append("🎯 Propósito: ").append(d.getProposito()).append("\n");
        }
        if (d.getLugar() != null) {
            sb.append("📌 Lugar: ").append(d.getLugar()).append("\n");
        }

        sb.append("\n¿Qué quieres hacer?\n\n");
        sb.append("• *enviar* → derivar a la Contadora\n");
        sb.append("• *devolver* → regresar al funcionario con observaciones");
        return sb.toString();
    }

    private String enviarAContadora(SesionUsuario sesion) {
        if (sesion.getTramiteEnRevision() == null) {
            return "⚠️ No has seleccionado ninguna liquidación.";
        }

        Long tramiteId = sesion.getTramiteEnRevision();
        tramiteService.actualizarEstado(tramiteId, "Liquidación revisada");

        HistorialEstadoRequestDTO historialDTO = new HistorialEstadoRequestDTO();
        historialDTO.setTramiteId(tramiteId);
        historialDTO.setEstadoAnterior("Liquidación en proceso");
        historialDTO.setEstadoNuevo("Liquidación revisada");
        historialDTO.setUsuarioId(sesion.getUsuarioId());
        historialDTO.setObservacion("Revisado por el Asistente Contable");
        historialEstadoService.guardar(historialDTO);

        // ✅ NOTIFICAR A LA CONTADORA (dinámico)
        String correoContadora = usuarioService.obtenerCorreoPorRol("Contadora");
        if (correoContadora == null) {
            System.err.println("⚠️ No se encontró una Contadora en la BD.");
        } else {
            NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
            notifDTO.setTramiteId(tramiteId);
            notifDTO.setDestinatario(correoContadora);
            notifDTO.setTipo("Liquidación pendiente de aprobación");
            notificacionService.guardar(notifDTO);
        }

        sesion.setTramiteEnRevision(null);

        return "✅ Liquidación *" + tramiteId + "* enviada a la Contadora.\n\n" +
                "📬 Se ha notificado a la *Contadora*.";
    }

    private String iniciarDevolucion(SesionUsuario sesion) {
        if (sesion.getTramiteEnRevision() == null) {
            return "⚠️ No has seleccionado ninguna liquidación.";
        }
        sesion.setEsperandoMotivoRechazo(true);
        return "✍️ ¿Cuál es la observación para devolver la liquidación al funcionario?";
    }

    private String devolverAlFuncionario(String observacion, SesionUsuario sesion) {
        sesion.setMotivoRechazo(observacion);
        sesion.setEsperandoMotivoRechazo(false);

        Long tramiteId = sesion.getTramiteEnRevision();
        tramiteService.actualizarEstado(tramiteId, "Devuelto al funcionario");

        HistorialEstadoRequestDTO historialDTO = new HistorialEstadoRequestDTO();
        historialDTO.setTramiteId(tramiteId);
        historialDTO.setEstadoAnterior("Liquidación en proceso");
        historialDTO.setEstadoNuevo("Devuelto al funcionario");
        historialDTO.setUsuarioId(sesion.getUsuarioId());
        historialDTO.setObservacion("Devuelto por el Asistente Contable: " + observacion);
        historialEstadoService.guardar(historialDTO);

        // ✅ NOTIFICAR AL FUNCIONARIO ORIGINAL (dinámico)
        String correoFuncionario = tramiteService.obtenerDetalle(tramiteId)
                .map(TramiteDetalleDTO::getCorreoUsuario)
                .orElse(null);

        if (correoFuncionario == null) {
            System.err.println("⚠️ No se encontró el correo del funcionario del trámite " + tramiteId);
        } else {
            NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
            notifDTO.setTramiteId(tramiteId);
            notifDTO.setDestinatario(correoFuncionario);
            notifDTO.setTipo("Liquidación devuelta para corrección");
            notificacionService.guardar(notifDTO);
        }

        sesion.setTramiteEnRevision(null);
        sesion.setMotivoRechazo(null);

        return "↩️ Liquidación *" + tramiteId + "* devuelta al funcionario.\n\n" +
                "📌 Observación: " + observacion + "\n\n" +
                "📬 Se ha notificado al *Funcionario*.";
    }
}