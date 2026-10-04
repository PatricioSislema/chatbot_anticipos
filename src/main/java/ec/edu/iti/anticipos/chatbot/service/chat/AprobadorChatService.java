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
 * Servicio común para los roles aprobadores: Jefe, Director y Contadora.
 * La lógica es idéntica, solo cambia la configuración (estados, destinatarios).
 */
@Service
@RequiredArgsConstructor
public class AprobadorChatService {

    private final TramiteService tramiteService;
    private final NotificacionService notificacionService;
    private final HistorialEstadoService historialEstadoService;

    private static final int MAX_PROPOSITO = 30;

    // Procesa los mensajes del aprobador
    public String procesar(String mensaje, String mensajeLower, SesionUsuario sesion,
                           ConfiguracionAprobador config) {

        if (sesion.isEsperandoMotivoRechazo()) {
            return procesarMotivoRechazo(mensaje, sesion, config);
        }

        // Aceptar "revisar ID"
        if (mensajeLower.matches("revisar\\s+\\d+")) {
            return seleccionarTramite(mensaje, sesion);
        }

        // Aceptar solo el número (ej. "21")
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

    // Lista los trámites pendientes con nombre + propósito truncado
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

            // Agregar el propósito si existe
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

    // Selecciona un trámite (versión vieja que recibe "revisar 15")
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

    // Selecciona un trámite por ID y muestra su detalle completo
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

    // Aprueba el trámite y notifica al rol específico
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

        NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
        notifDTO.setTramiteId(tramiteId);
        notifDTO.setDestinatario(config.getDestinatarioAprobacion());
        notifDTO.setTipo("Trámite aprobado por " + config.getRolNombre());
        notificacionService.guardar(notifDTO);

        sesion.setTramiteEnRevision(null);

        // Mensaje con nombre del siguiente rol
        return "✅ Trámite *" + tramiteId + "* aprobado correctamente.\n\n" +
                "📬 Se ha notificado al *" + config.getNombreSiguienteRol() + "*.";
    }

    // Inicia el flujo de rechazo (pide motivo)
    private String iniciarRechazo(SesionUsuario sesion) {
        if (sesion.getTramiteEnRevision() == null) {
            return "⚠️ No has seleccionado ningún trámite.\n\n" +
                    "📌 Primero debes elegir uno de la lista.\n\n" +
                    "👉 Escribe *revisar ID* o solo el número para seleccionarlo.";
        }
        sesion.setEsperandoMotivoRechazo(true);
        return "✍️ ¿Cuál es el motivo del rechazo?\n\n" +
                "👉 Escribe el motivo y se enviará al docente.";
    }

    // Procesa el motivo del rechazo y rechaza el trámite
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

        NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
        notifDTO.setTramiteId(tramiteId);
        notifDTO.setDestinatario(config.getDestinatarioRechazo());
        notifDTO.setTipo("Trámite rechazado por " + config.getRolNombre());
        notificacionService.guardar(notifDTO);

        sesion.setTramiteEnRevision(null);
        sesion.setMotivoRechazo(null);

        return "❌ Trámite *" + tramiteId + "* rechazado.\n\n" +
                "📌 Motivo: " + motivo + "\n\n" +
                "📬 Se ha notificado al *Docente*.";
    }
}