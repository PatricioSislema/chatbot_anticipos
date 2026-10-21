package ec.edu.iti.anticipos.chatbot.service.chat;

import ec.edu.iti.anticipos.chatbot.dto.request.HistorialEstadoRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.request.NotificacionRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.request.OrdenTransferenciaRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.OrdenTransferenciaResponseDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.ReembolsoResponseDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.TramiteDetalleDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.TramiteResponseDTO;
import ec.edu.iti.anticipos.chatbot.service.HistorialEstadoService;
import ec.edu.iti.anticipos.chatbot.service.NotificacionService;
import ec.edu.iti.anticipos.chatbot.service.OrdenTransferenciaService;
import ec.edu.iti.anticipos.chatbot.service.ReembolsoService;
import ec.edu.iti.anticipos.chatbot.service.TramiteService;
import ec.edu.iti.anticipos.chatbot.service.UsuarioService;
import ec.edu.iti.anticipos.chatbot.sesion.SesionUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ec.edu.iti.anticipos.chatbot.util.MensajeHelper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Servicio especializado en la lógica de la Colectora.
 *
 * Maneja DOS flujos:
 *
 *  * FLUJO ANTICIPO (solicitud original):
 *  *  1. Contadora aprueba → trámite en "Aprobado por Contadora".
 *  *  2. Colectora genera orden → trámite en "Orden generada".
 *  *  3. Director aprueba orden → trámite en "Orden aprobada" → dinero se efectúa.
 *  *  4. Contadora registra el comprobante → trámite en "Transferido".
 *  *  5. Notifica al Funcionario y Director.
 *  *
 *  * FLUJO REEMBOLSO (caso FALTANTE):
 *  *  1. Director autoriza reembolso → Reembolso en estado "Autorizado".
 *  *  2. Colectora genera orden → trámite en "Orden generada".
 *  *  3. Director aprueba orden → trámite en "Orden aprobada" → dinero se efectúa.
 *  *  4. Contadora cierra reembolso → trámite en "Liquidación cerrada".
 */
@Service
@RequiredArgsConstructor
public class ColectoraChatService {

    private final TramiteService tramiteService;
    private final NotificacionService notificacionService;
    private final HistorialEstadoService historialEstadoService;
    private final UsuarioService usuarioService;
    private final OrdenTransferenciaService ordenTransferenciaService;
    private final ReembolsoService reembolsoService; // ✅ NUEVO

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

        // Generar orden
        if (mensajeLower.contains("generar") || mensajeLower.contains("orden")) {
            return generarOrden(sesion);
        }

        // ✅ Validación de texto inválido (al final, solo si nada más coincide)
        if (sesion.getTramiteEnRevision() == null
                && !mensajeLower.isEmpty()
                && !mensajeLower.matches("\\d+")
                && !mensajeLower.contains("menu")
                && !mensajeLower.contains("menú")
                && !mensajeLower.contains("ayuda")
                && !mensajeLower.contains("cancelar")
                && !mensajeLower.contains("salir")
                && !mensajeLower.contains("pendiente")
                && !mensajeLower.contains("revisar")) {
            return "⚠️ Por favor, ingresa solo el número del trámite que deseas gestionar.\n\n" +
                    "👉 Ejemplo: *13*";
        }

        return null;
    }

    // ============================================================
    // LISTAR PENDIENTES (anticipos + reembolsos)
    // ============================================================
    private String listarPendientes() {
        // 1. Anticipos aprobados por Contadora (para generar orden)
        List<TramiteResponseDTO> anticipos = tramiteService.listarPorEstado("Aprobado por Contadora");

        // 2. Reembolsos autorizados (para generar orden)
        List<ReembolsoResponseDTO> reembolsos = reembolsoService.listarPorEstado("Autorizado");

        if (anticipos.isEmpty() && reembolsos.isEmpty()) {
            return "📋 No hay transferencias pendientes.\n\n" +
                    "👉 Escribe *menu* para volver al menú principal.";
        }

        StringBuilder lista = new StringBuilder();

        // Sección de anticipos
        if (!anticipos.isEmpty()) {
            lista.append("📋 *Anticipos pendientes:* (").append(anticipos.size()).append(")\n\n");
            for (TramiteResponseDTO t : anticipos) {
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

        // Sección de reembolsos
        if (!reembolsos.isEmpty()) {
            lista.append("\n💰 *Reembolsos autorizados:* (").append(reembolsos.size()).append(")\n\n");
            for (ReembolsoResponseDTO r : reembolsos) {
                lista.append("🆔 Trámite #").append(r.getTramiteId())
                        .append(" — Reembolso #").append(r.getReembolsoId())
                        .append(" — $").append(r.getMontoSolicitado())
                        .append("\n");
            }
        }

        lista.append("\n👉 Escribe el *número del trámite* que deseas gestionar.");
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
            return "⚠️ Formato incorrecto.";
        }
    }

    private String seleccionarTramitePorId(Long tramiteId, SesionUsuario sesion) {
        sesion.setTramiteEnRevision(tramiteId);

        Optional<TramiteDetalleDTO> detalleOpt = tramiteService.obtenerDetalle(tramiteId);
        if (detalleOpt.isEmpty()) {
            return "⚠️ Trámite no encontrado.\n\n" +
                    "📌 Por favor, ingresa el número de trámite que deseas registrar.\n\n" +
                    "👉 Ejemplo: *13*";
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

        // ✅ Detectar si es un reembolso
        Optional<ReembolsoResponseDTO> reembolsoOpt = reembolsoService.buscarPorTramite(tramiteId);
        boolean esReembolso = reembolsoOpt.isPresent()
                && "Autorizado".equals(reembolsoOpt.get().getEstado());

        // Si ya tiene orden de transferencia, mostrar su estado
        Optional<OrdenTransferenciaResponseDTO> ordenOpt = ordenTransferenciaService.buscarPorTramite(tramiteId);
        if (ordenOpt.isPresent()) {
            OrdenTransferenciaResponseDTO orden = ordenOpt.get();
            sb.append("\n📄 *Orden de transferencia:*\n");
            sb.append("🆔 Orden #").append(orden.getOrdenId()).append("\n");
            sb.append("📊 Estado: *").append(orden.getEstado()).append("*\n");
            sb.append("💰 Valor: $").append(orden.getValor()).append("\n");
            if (orden.getDirectorNombre() != null) {
                sb.append("👤 Director: ").append(orden.getDirectorNombre()).append("\n");
            }
            sb.append("\n");

            if ("Generada".equals(orden.getEstado())) {
                sb.append("⏳ La orden está pendiente de aprobación del Director.");
            } else if ("Aprobada".equals(orden.getEstado())) {
                sb.append("✅ La orden está aprobada. Puedes *ejecutar la transferencia*.");
            }
        } else if (esReembolso) {
            // ✅ Es un reembolso autorizado, no tiene orden aún
            ReembolsoResponseDTO reembolso = reembolsoOpt.get();
            sb.append("\n💰 *Reembolso autorizado:* #").append(reembolso.getReembolsoId()).append("\n");
            sb.append("💵 Monto: $").append(reembolso.getMontoSolicitado()).append("\n");
            sb.append("\n¿Generar la orden de reembolso?\n\n");
            sb.append("Escribe *generar orden*.");
        } else {
            sb.append("\n¿Generar la orden de transferencia?\n\n");
            sb.append("Escribe *generar orden*.");
        }

        return sb.toString();
    }

    // ============================================================
    // GENERAR ORDEN (anticipo o reembolso)
    // ============================================================
    private String generarOrden(SesionUsuario sesion) {
        if (sesion.getTramiteEnRevision() == null) {
            return "⚠️ No has seleccionado ningún trámite.";
        }

        Long tramiteId = sesion.getTramiteEnRevision();

        // Verificar que no exista ya una orden para este trámite
        Optional<OrdenTransferenciaResponseDTO> ordenExistente = ordenTransferenciaService.buscarPorTramite(tramiteId);
        if (ordenExistente.isPresent()) {
            return "⚠️ Ya existe una orden de transferencia para este trámite.\n\n" +
                    "📊 Estado actual: *" + ordenExistente.get().getEstado() + "*";
        }

        // ✅ Detectar si es reembolso
        Optional<ReembolsoResponseDTO> reembolsoOpt = reembolsoService.buscarPorTramite(tramiteId);
        boolean esReembolso = reembolsoOpt.isPresent()
                && "Autorizado".equals(reembolsoOpt.get().getEstado());

        // Obtener datos del trámite
        Optional<TramiteDetalleDTO> detalleOpt = tramiteService.obtenerDetalle(tramiteId);
        if (detalleOpt.isEmpty()) {
            return "⚠️ No encontré el trámite.";
        }
        TramiteDetalleDTO detalle = detalleOpt.get();

        // Determinar el valor y el concepto según el tipo
        BigDecimal valor;
        String concepto;

        if (esReembolso) {
            ReembolsoResponseDTO reembolso = reembolsoOpt.get();
            valor = reembolso.getMontoSolicitado();
            concepto = "Reembolso al funcionario - Trámite #" + tramiteId;
        } else {
            valor = detalle.getPresupuesto();
            concepto = "Anticipo para: " + detalle.getProposito();
        }

        // Crear la orden
        OrdenTransferenciaRequestDTO ordenDTO = new OrdenTransferenciaRequestDTO();
        ordenDTO.setTramiteId(tramiteId);
        ordenDTO.setColectoraId(sesion.getUsuarioId());
        ordenDTO.setNumeroCuenta("1234567890");  // Simulado
        ordenDTO.setValor(valor);
        ordenDTO.setConcepto(concepto);
        ordenDTO.setFechaGeneracion(LocalDate.now());
        ordenDTO.setEstado("Generada");

        OrdenTransferenciaResponseDTO orden = ordenTransferenciaService.guardar(ordenDTO);

        // Historial
        String estadoAnterior = esReembolso ? "Reembolso autorizado" : "Aprobado por Contadora";
        HistorialEstadoRequestDTO historialDTO = new HistorialEstadoRequestDTO();
        historialDTO.setTramiteId(tramiteId);
        historialDTO.setEstadoAnterior(estadoAnterior);
        historialDTO.setEstadoNuevo("Orden generada");
        historialDTO.setUsuarioId(sesion.getUsuarioId());
        historialDTO.setObservacion("Orden de transferencia #" + orden.getOrdenId()
                + " generada (" + (esReembolso ? "REEMBOLSO" : "ANTICIPO") + ")");
        historialEstadoService.guardar(historialDTO);

        // Notificar al Director Financiero
        String correoDirector = usuarioService.obtenerCorreoPorRol("Director Financiero");
        if (correoDirector != null) {
            String mensaje = MensajeHelper.ordenGenerada(
                    orden.getOrdenId(), tramiteId,
                    orden.getValor().toString(),
                    esReembolso ? "REEMBOLSO" : "ANTICIPO");

            NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
            notifDTO.setTramiteId(tramiteId);
            notifDTO.setDestinatario(correoDirector);
            notifDTO.setTipo("Orden de transferencia pendiente de aprobación");
            notifDTO.setMensajePersonalizado(mensaje + "\n\n📌 Escribe: *aprobar orden " + orden.getOrdenId() + "*");
            notificacionService.guardar(notifDTO);
        }

        return "✅ Orden de transferencia generada correctamente.\n\n" +
                "🆔 Orden #" + orden.getOrdenId() + "\n" +
                "💰 Valor: $" + orden.getValor() + "\n" +
                "📊 Estado: *Generada*\n" +
                "🏷️ Tipo: *" + (esReembolso ? "Reembolso" : "Anticipo") + "*\n\n" +
                "⏳ El Director Financiero debe aprobarla.\n" +
                "📬 Se ha notificado al *Director Financiero*.";
    }

    // ============================================================
    // EJECUTAR TRANSFERENCIA (anticipo o reembolso)
    // ============================================================

}