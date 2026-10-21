package ec.edu.iti.anticipos.chatbot.service.chat;

import ec.edu.iti.anticipos.chatbot.dto.request.ComprobanteTransferenciaRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.request.HistorialEstadoRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.request.NotificacionRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.request.ReembolsoRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.DevolucionResponseDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.LiquidacionResponseDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.OrdenTransferenciaResponseDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.ReembolsoResponseDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.TramiteDetalleDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.TramiteResponseDTO;
import ec.edu.iti.anticipos.chatbot.service.ComprobanteTransferenciaService;
import ec.edu.iti.anticipos.chatbot.service.DevolucionService;
import ec.edu.iti.anticipos.chatbot.service.HistorialEstadoService;
import ec.edu.iti.anticipos.chatbot.service.LiquidacionService;
import ec.edu.iti.anticipos.chatbot.service.NotificacionService;
import ec.edu.iti.anticipos.chatbot.service.OrdenTransferenciaService;
import ec.edu.iti.anticipos.chatbot.service.ReembolsoService;
import ec.edu.iti.anticipos.chatbot.service.TramiteService;
import ec.edu.iti.anticipos.chatbot.service.UsuarioService;
import ec.edu.iti.anticipos.chatbot.sesion.SesionUsuario;
import ec.edu.iti.anticipos.chatbot.util.MensajeHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Servicio especializado en la lógica de la Contadora.
 *
 * Maneja:
 *  1. Aprobar solicitudes (después del Director).
 *  2. Aprobar liquidaciones (después del Asistente Contable) — con los 3 casos.
 *  3. VALIDAR las devoluciones (caso SOBRANTE) — cierra la liquidación.
 *  4. INFORMAR A LA COLECTORA para reembolsos (caso FALTANTE).
 *  5. REGISTRAR el comprobante de transferencia (FLUJO 1 - anticipo).
 *  6. CERRAR el reembolso (FLUJO 3 - FALTANTE).
 */
@Service
@RequiredArgsConstructor
public class ContadoraChatService {

    private final TramiteService tramiteService;
    private final NotificacionService notificacionService;
    private final HistorialEstadoService historialEstadoService;
    private final UsuarioService usuarioService;
    private final LiquidacionService liquidacionService;
    private final DevolucionService devolucionService;
    private final ReembolsoService reembolsoService;
    private final ComprobanteTransferenciaService comprobanteTransferenciaService;
    private final OrdenTransferenciaService ordenTransferenciaService;

    // ============================================================
    // PROCESAR MENSAJE
    // ============================================================
    public String procesar(String mensaje, String mensajeLower, SesionUsuario sesion) {

        // ✅ Registrar comprobante de transferencia (FLUJO 1 - anticipo)
        if (mensajeLower.matches("registrar\\s+comprobante\\s+\\d+")) {
            Long tramiteId = Long.parseLong(mensajeLower.replaceAll("[^0-9]", ""));
            return registrarComprobante(tramiteId, sesion);
        }

        if (sesion.isEsperandoMotivoRechazo()) {
            return procesarMotivoRechazo(mensaje, sesion);
        }

        // ✅ Informar a Colectora sobre reembolso autorizado
        if (mensajeLower.matches("informar\\s+colectora\\s+\\d+")) {
            Long tramiteId = Long.parseLong(mensajeLower.replaceAll("[^0-9]", ""));
            return informarColectora(tramiteId, sesion);
        }

        // ✅ Listar devoluciones pendientes de validar
        if (mensajeLower.equals("validar devoluciones")
                || mensajeLower.equals("devoluciones")
                || mensajeLower.contains("validar devolucion")) {

            if (mensajeLower.matches("validar\\s+devolucion\\s+\\d+")) {
                Long devolucionId = Long.parseLong(mensajeLower.replaceAll("[^0-9]", ""));
                return validarDevolucion(devolucionId, sesion);
            }

            return listarDevolucionesPorValidar();
        }

        // ✅ Rechazar devolución
        if (mensajeLower.matches("rechazar\\s+devolucion\\s+\\d+")) {
            Long devolucionId = Long.parseLong(mensajeLower.replaceAll("[^0-9]", ""));
            return iniciarRechazoDevolucion(devolucionId, sesion);
        }

        // ✅ Enviar comprobante al funcionario
        if (mensajeLower.matches("enviar\\s+comprobante\\s+\\d+")) {
            Long tramiteId = Long.parseLong(mensajeLower.replaceAll("[^0-9]", ""));
            return enviarComprobante(tramiteId, sesion);
        }

        if (mensajeLower.equals("comprobantes")) {
            return listarComprobantesPorEnviar();
        }

        // ✅ Cerrar reembolso (FLUJO 3 - FALTANTE)
        if (mensajeLower.matches("cerrar\\s+reembolso\\s+\\d+")) {
            Long reembolsoId = Long.parseLong(mensajeLower.replaceAll("[^0-9]", ""));
            return cerrarReembolso(reembolsoId, sesion);
        }

        if (mensajeLower.equals("reembolsos")) {
            return listarReembolsosAutorizados();
        }

        // Aceptar "seleccionar ID"
        if (mensajeLower.matches("seleccionar\\s+\\d+")) {
            return seleccionarTramite(mensaje, sesion);
        }

        if (mensajeLower.matches("\\d+")) {
            return seleccionarTramitePorId(Long.parseLong(mensajeLower), sesion);
        }

        if (mensajeLower.contains("solicitud") && mensajeLower.contains("pendiente")) {
            return listarSolicitudesPendientes();
        }

        if (mensajeLower.contains("liquidacion") && mensajeLower.contains("pendiente")) {
            return listarLiquidacionesPendientes();
        }

        if (mensajeLower.contains("pendiente") || mensajeLower.contains("revisar")) {
            return listarTodo();
        }

        if (mensajeLower.contains("aprobar")) {
            return aprobar(sesion);
        }

        if (mensajeLower.contains("rechazar")) {
            return iniciarRechazo(sesion);
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
                && !mensajeLower.contains("aprobar")
                && !mensajeLower.contains("rechazar")
                && !mensajeLower.contains("validar")
                && !mensajeLower.contains("devolucion")
                && !mensajeLower.contains("devolución")
                && !mensajeLower.contains("reembolso")
                && !mensajeLower.contains("comprobante")
                && !mensajeLower.contains("informar")
                && !mensajeLower.contains("cerrar")) {
            return "⚠️ Por favor, ingresa un número válido según la acción que estés realizando:\n\n" +
                    "• Para revisar un trámite: *el número del trámite*\n" +
                    "• Para validar una devolución: *el número de la devolución*\n" +
                    "• Para cerrar un reembolso: *el número del reembolso*";
        }

        return null;
    }

    // ============================================================
    // REGISTRAR COMPROBANTE DE TRANSFERENCIA (FLUJO 1)
    // ============================================================
    private String registrarComprobante(Long tramiteId, SesionUsuario sesion) {
        Optional<TramiteDetalleDTO> detalleOpt = tramiteService.obtenerDetalle(tramiteId);
        if (detalleOpt.isEmpty()) {
            return "⚠️ No encontré el trámite #" + tramiteId + ".";
        }

        TramiteDetalleDTO detalle = detalleOpt.get();

        if (!"Orden aprobada".equals(detalle.getEstado())) {
            return "⚠️ El trámite #" + tramiteId + " no está listo para registrar comprobante.\n\n" +
                    "📊 Estado actual: *" + detalle.getEstado() + "*\n\n" +
                    "👉 El Director debe aprobar la orden primero.";
        }

        // Buscar la orden de transferencia para obtener el monto
        Optional<OrdenTransferenciaResponseDTO> ordenOpt =
                ordenTransferenciaService.buscarPorTramite(tramiteId);

        BigDecimal monto = detalle.getPresupuesto();
        if (ordenOpt.isPresent() && ordenOpt.get().getValor() != null) {
            monto = ordenOpt.get().getValor();
        }

        // Cambiar trámite a "Transferido"
        tramiteService.actualizarEstado(tramiteId, "Transferido");

        // Historial
        HistorialEstadoRequestDTO historialDTO = new HistorialEstadoRequestDTO();
        historialDTO.setTramiteId(tramiteId);
        historialDTO.setEstadoAnterior("Orden aprobada");
        historialDTO.setEstadoNuevo("Transferido");
        historialDTO.setUsuarioId(sesion.getUsuarioId());
        historialDTO.setObservacion("Comprobante registrado por la Contadora");
        historialEstadoService.guardar(historialDTO);

        // Registrar comprobante
        ComprobanteTransferenciaRequestDTO comprobanteDTO = new ComprobanteTransferenciaRequestDTO();
        comprobanteDTO.setTramiteId(tramiteId);
        comprobanteDTO.setTipo("ANTICIPO");
        comprobanteDTO.setArchivoPdf("comprobante_" + tramiteId + ".pdf");
        comprobanteDTO.setMonto(monto);
        comprobanteDTO.setFecha(LocalDate.now());
        comprobanteTransferenciaService.guardar(comprobanteDTO);

        // Notificar al Funcionario y al Director
        String correoFuncionario = tramiteService.obtenerDetalle(tramiteId)
                .map(TramiteDetalleDTO::getCorreoUsuario)
                .orElse(null);
        String correoDirector = usuarioService.obtenerCorreoPorRol("Director Financiero");

        String mensaje = MensajeHelper.comprobanteRegistrado(tramiteId, monto.toString());

        if (correoFuncionario != null) {
            NotificacionRequestDTO notifFuncionario = new NotificacionRequestDTO();
            notifFuncionario.setTramiteId(tramiteId);
            notifFuncionario.setDestinatario(correoFuncionario);
            notifFuncionario.setTipo("Anticipo transferido");
            notifFuncionario.setMensajePersonalizado(mensaje);
            notificacionService.guardar(notifFuncionario);
        }

        if (correoDirector != null) {
            NotificacionRequestDTO notifDirector = new NotificacionRequestDTO();
            notifDirector.setTramiteId(tramiteId);
            notifDirector.setDestinatario(correoDirector);
            notifDirector.setTipo("Comprobante registrado - Anticipo transferido");
            notifDirector.setMensajePersonalizado(mensaje);
            notificacionService.guardar(notifDirector);
        }

        return "✅ Comprobante registrado correctamente.\n\n" +
                "🆔 Trámite: #" + tramiteId + "\n" +
                "💰 Monto: $" + monto + "\n" +
                "📊 Estado: *Transferido*\n\n" +
                "📬 Se ha notificado al *Funcionario* y al *Director Financiero*.";
    }

    // ============================================================
    // INFORMAR A COLECTORA (FALTANTE)
    // ============================================================
    private String informarColectora(Long tramiteId, SesionUsuario sesion) {
        Optional<ReembolsoResponseDTO> reembolsoOpt = reembolsoService.buscarPorTramite(tramiteId);

        if (reembolsoOpt.isEmpty()) {
            return "⚠️ No encontré un reembolso para el trámite #" + tramiteId + ".";
        }

        ReembolsoResponseDTO reembolso = reembolsoOpt.get();

        if (!"Autorizado".equals(reembolso.getEstado())) {
            return "⚠️ El reembolso #" + reembolso.getReembolsoId() + " no está autorizado.\n\n" +
                    "📊 Estado actual: *" + reembolso.getEstado() + "*\n\n" +
                    "👉 El Director Financiero debe autorizarlo primero.";
        }

        // Notificar a la Colectora
        String correoColectora = usuarioService.obtenerCorreoPorRol("Colectora");
        if (correoColectora == null) {
            return "⚠️ No se encontró una Colectora en la BD.";
        }

        String mensaje = MensajeHelper.informarColectora(tramiteId, reembolso.getReembolsoId(),
                reembolso.getMontoSolicitado().toString());

        NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
        notifDTO.setTramiteId(tramiteId);
        notifDTO.setDestinatario(correoColectora);
        notifDTO.setTipo("Reembolso autorizado - Generar orden de transferencia");
        notifDTO.setMensajePersonalizado(mensaje + "\n\n📌 Escribe: *" + tramiteId + "* para generar la orden.");
        notificacionService.guardar(notifDTO);

        // Historial
        HistorialEstadoRequestDTO historialDTO = new HistorialEstadoRequestDTO();
        historialDTO.setTramiteId(tramiteId);
        historialDTO.setEstadoAnterior("Reembolso autorizado");
        historialDTO.setEstadoNuevo("Esperando orden de reembolso");
        historialDTO.setUsuarioId(sesion.getUsuarioId());
        historialDTO.setObservacion("Contadora informó a la Colectora sobre el reembolso autorizado");
        historialEstadoService.guardar(historialDTO);

        return "✅ Se ha informado a la *Colectora*.\n\n" +
                "💰 Reembolso #" + reembolso.getReembolsoId() + " — $" + reembolso.getMontoSolicitado() + "\n" +
                "🆔 Trámite: #" + tramiteId + "\n\n" +
                "📬 La Colectora debe generar la orden de transferencia.\n\n" +
                "📌 Cuando el Director apruebe la orden, escribe *cerrar reembolso "
                + reembolso.getReembolsoId() + "*.";
    }

    // ============================================================
    // CERRAR REEMBOLSO (FLUJO 3 - FALTANTE)
    // ============================================================
    private String cerrarReembolso(Long reembolsoId, SesionUsuario sesion) {
        Optional<ReembolsoResponseDTO> reembolsoOpt = reembolsoService.buscarPorId(reembolsoId);

        if (reembolsoOpt.isEmpty()) {
            return "⚠️ No encontré el reembolso #" + reembolsoId + ".";
        }

        ReembolsoResponseDTO reembolso = reembolsoOpt.get();

        if (!"Autorizado".equals(reembolso.getEstado())) {
            return "⚠️ El reembolso #" + reembolsoId + " no está listo para cerrar.\n\n" +
                    "📊 Estado actual: *" + reembolso.getEstado() + "*";
        }

        // Cerrar reembolso
        reembolsoService.cerrar(reembolsoId);

        // Cerrar liquidación
        Long tramiteId = reembolso.getTramiteId();
        tramiteService.actualizarEstado(tramiteId, "Liquidación cerrada");

        // Historial
        HistorialEstadoRequestDTO historialDTO = new HistorialEstadoRequestDTO();
        historialDTO.setTramiteId(tramiteId);
        historialDTO.setEstadoAnterior("Reembolso autorizado");
        historialDTO.setEstadoNuevo("Liquidación cerrada");
        historialDTO.setUsuarioId(sesion.getUsuarioId());
        historialDTO.setObservacion("Reembolso cerrado y liquidación finalizada (caso FALTANTE)");
        historialEstadoService.guardar(historialDTO);

        // Notificar al Funcionario y al Director
        String correoFuncionario = tramiteService.obtenerDetalle(tramiteId)
                .map(TramiteDetalleDTO::getCorreoUsuario)
                .orElse(null);
        String correoDirector = usuarioService.obtenerCorreoPorRol("Director Financiero");

        String mensaje = MensajeHelper.reembolsoCerrado(tramiteId, reembolso.getMontoSolicitado().toString());

        if (correoFuncionario != null) {
            NotificacionRequestDTO notifFuncionario = new NotificacionRequestDTO();
            notifFuncionario.setTramiteId(tramiteId);
            notifFuncionario.setDestinatario(correoFuncionario);
            notifFuncionario.setTipo("Liquidación cerrada - Reembolso procesado");
            notifFuncionario.setMensajePersonalizado(mensaje);
            notificacionService.guardar(notifFuncionario);
        }

        if (correoDirector != null) {
            NotificacionRequestDTO notifDirector = new NotificacionRequestDTO();
            notifDirector.setTramiteId(tramiteId);
            notifDirector.setDestinatario(correoDirector);
            notifDirector.setTipo("Liquidación cerrada - Reembolso procesado");
            notifDirector.setMensajePersonalizado(mensaje);
            notificacionService.guardar(notifDirector);
        }

        return "✅ *Reembolso #" + reembolsoId + " cerrado.*\n\n" +
                "💰 Monto: $" + reembolso.getMontoSolicitado() + "\n" +
                "🎉 La liquidación del trámite *#" + tramiteId + "* ha sido cerrada.\n\n" +
                "📬 Se ha notificado al *Funcionario* y al *Director Financiero*.";
    }

    // ============================================================
    // LISTAR REEMBOLSOS AUTORIZADOS
    // ============================================================
    private String listarReembolsosAutorizados() {
        List<ReembolsoResponseDTO> reembolsos = reembolsoService.listarPorEstado("Autorizado");

        if (reembolsos.isEmpty()) {
            return "📋 No hay reembolsos autorizados pendientes de cerrar.\n\n" +
                    "👉 Escribe *menu* para volver al menú principal.";
        }

        StringBuilder lista = new StringBuilder("📋 *Reembolsos autorizados:* (" + reembolsos.size() + ")\n\n");
        for (ReembolsoResponseDTO r : reembolsos) {
            lista.append("🆔 Reembolso #").append(r.getReembolsoId())
                    .append(" — Trámite #").append(r.getTramiteId())
                    .append(" — $").append(r.getMontoSolicitado())
                    .append("\n");
        }
        lista.append("\n👉 Escribe *cerrar reembolso ID* cuando el Director haya aprobado la orden.");
        return lista.toString();
    }

    // ============================================================
    // LISTAR PENDIENTES
    // ============================================================
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
        lista.append("\n👉 Escribe el *número de la solicitud* que deseas revisar.");
        lista.append("\nEjemplo: *13*.");
        return lista.toString();
    }

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
        lista.append("\n👉 Escribe el *número de la liquidación* que deseas revisar.");
        lista.append("\nEjemplo: *13*.");
        return lista.toString();
    }

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

        lista.append("\n👉 Escribe el *número del trámite* que deseas revisar.");
        lista.append("\nEjemplo: *13*.");
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
                    "📌 Debes escribir *seleccionar* seguido del número.\n\n" +
                    "👉 Ejemplo: seleccionar 15";
        }
    }

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

        if (d.getPresupuesto() != null) {
            sb.append("\n💰 Presupuesto: $").append(d.getPresupuesto()).append("\n");
            if (d.getUnidad() != null) sb.append("🏢 Unidad: ").append(d.getUnidad()).append("\n");
            if (d.getCampus() != null) sb.append("📍 Campus: ").append(d.getCampus()).append("\n");
            if (d.getProposito() != null) sb.append("🎯 Propósito: ").append(d.getProposito()).append("\n");
            if (d.getLugar() != null) sb.append("📌 Lugar: ").append(d.getLugar()).append("\n");
            if (d.getFechaRealizacion() != null) sb.append("📅 Fecha realización: ").append(d.getFechaRealizacion()).append("\n");
        }

        if (d.getTotalGastado() != null) {
            sb.append("\n💰 Total gastado: $").append(d.getTotalGastado()).append("\n");
            if (d.getSaldo() != null) {
                sb.append("📈 Saldo: $").append(d.getSaldo()).append("\n");

                String tipoCaso = liquidacionService.detectarTipoCaso(d.getSaldo());
                sb.append("📊 Tipo de caso: *").append(tipoCaso).append("*\n");
            }
            if (d.getFechaPresentacion() != null) sb.append("📅 Fecha presentación: ").append(d.getFechaPresentacion()).append("\n");
        }

        sb.append("\n¿Qué deseas hacer con este trámite?");
        sb.append("\n👉 Usa los botones de abajo para continuar.");
        return sb.toString();
    }

    // ============================================================
    // APROBAR
    // ============================================================
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

    private String aprobarSolicitud(Long tramiteId, SesionUsuario sesion) {
        tramiteService.actualizarEstado(tramiteId, "Aprobado por Contadora");

        HistorialEstadoRequestDTO historialDTO = new HistorialEstadoRequestDTO();
        historialDTO.setTramiteId(tramiteId);
        historialDTO.setEstadoAnterior("Aprobado por Director");
        historialDTO.setEstadoNuevo("Aprobado por Contadora");
        historialDTO.setUsuarioId(sesion.getUsuarioId());
        historialDTO.setObservacion("Aprobado por la Contadora");
        historialEstadoService.guardar(historialDTO);

        String correoColectora = usuarioService.obtenerCorreoPorRol("Colectora");
        if (correoColectora == null) {
            System.err.println("⚠️ No se encontró una Colectora en la BD.");
        } else {
            String nombreSolicitante = tramiteService.obtenerDetalle(tramiteId)
                    .map(TramiteDetalleDTO::getNombreUsuario)
                    .orElse("N/A");

            String mensaje = MensajeHelper.tramiteAprobado(tramiteId, nombreSolicitante, "Aprobado por Contadora");

            NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
            notifDTO.setTramiteId(tramiteId);
            notifDTO.setDestinatario(correoColectora);
            notifDTO.setTipo("Solicitud aprobada por Contadora");
            notifDTO.setMensajePersonalizado(mensaje + "\n\n📌 Escribe: *" + tramiteId + "* para generar la orden.");
            notificacionService.guardar(notifDTO);
        }

        sesion.setTramiteEnRevision(null);
        return "✅ Solicitud *" + tramiteId + "* aprobada correctamente.\n\n" +
                "📬 Se ha notificado a la *Colectora*.";
    }

    private String aprobarLiquidacion(Long tramiteId, SesionUsuario sesion) {
        Optional<LiquidacionResponseDTO> liquidacionOpt = liquidacionService.buscarPorTramite(tramiteId);

        if (liquidacionOpt.isEmpty()) {
            return "⚠️ No encontré la liquidación del trámite *" + tramiteId + "*.";
        }

        LiquidacionResponseDTO liquidacion = liquidacionOpt.get();
        BigDecimal saldo = liquidacion.getSaldo();

        String tipoCaso = liquidacionService.detectarTipoCaso(saldo);
        liquidacionService.actualizarTipoCaso(liquidacion.getLiquidacionId(), tipoCaso);

        tramiteService.actualizarEstado(tramiteId, "Liquidación aprobada");

        HistorialEstadoRequestDTO historialDTO = new HistorialEstadoRequestDTO();
        historialDTO.setTramiteId(tramiteId);
        historialDTO.setEstadoAnterior("Liquidación revisada");
        historialDTO.setEstadoNuevo("Liquidación aprobada");
        historialDTO.setUsuarioId(sesion.getUsuarioId());
        historialDTO.setObservacion("Liquidación aprobada por la Contadora. Tipo de caso: " + tipoCaso);
        historialEstadoService.guardar(historialDTO);

        String correoFuncionario = tramiteService.obtenerDetalle(tramiteId)
                .map(TramiteDetalleDTO::getCorreoUsuario)
                .orElse(null);

        String correoDirector = usuarioService.obtenerCorreoPorRol("Director Financiero");

        StringBuilder respuesta = new StringBuilder();
        respuesta.append("✅ Liquidación *").append(tramiteId).append("* aprobada correctamente.\n\n");
        respuesta.append("📊 Tipo de caso: *").append(tipoCaso).append("*\n");
        respuesta.append("💰 Saldo: $").append(saldo).append("\n\n");

        switch (tipoCaso) {
            case "SOBRANTE":
                Optional<DevolucionResponseDTO> devolucionOpt = devolucionService.buscarPorTramite(tramiteId);

                if (devolucionOpt.isPresent()) {
                    DevolucionResponseDTO devolucion = devolucionOpt.get();
                    respuesta.append("💸 *Hay una devolución pendiente de validar.*\n");
                    respuesta.append("🆔 Devolución: #").append(devolucion.getDevolucionId()).append("\n");
                    respuesta.append("💰 Monto: $").append(devolucion.getMonto()).append("\n");
                    respuesta.append("📊 Estado: *").append(devolucion.getEstado()).append("*\n\n");
                    respuesta.append("👉 Escribe *validar devolucion ").append(devolucion.getDevolucionId()).append("*");
                    respuesta.append(" para validarla y cerrar la liquidación.\n");
                    respuesta.append("👉 O escribe *rechazar devolucion ").append(devolucion.getDevolucionId()).append("*");
                    respuesta.append(" si el comprobante no es válido.");
                } else {
                    respuesta.append("⚠️ No encontré la devolución asociada al trámite.\n\n");
                    respuesta.append("📌 El funcionario debió subir el comprobante al liquidar.\n");
                    respuesta.append("👉 Contacta al funcionario para que suba el comprobante.");
                }
                break;

            case "FALTANTE":
                ReembolsoRequestDTO reembolsoDTO = new ReembolsoRequestDTO();
                reembolsoDTO.setLiquidacionId(liquidacion.getLiquidacionId());
                reembolsoDTO.setMontoSolicitado(saldo.abs());
                reembolsoDTO.setFechaSolicitud(LocalDate.now());
                reembolsoDTO.setEstado("Pendiente");
                reembolsoDTO.setObservacion("Reembolso generado por la Contadora tras aprobar la liquidación");
                reembolsoService.guardar(reembolsoDTO);

                respuesta.append("💸 Se ha creado un *reembolso pendiente*.\n");
                respuesta.append("📌 El ITI debe reembolsar $").append(saldo.abs()).append(" al funcionario.\n");
                respuesta.append("📋 Este reembolso debe ser autorizado por el *Director Financiero*.\n\n");

                if (correoDirector != null) {
                    String mensaje = MensajeHelper.liquidacionAprobada(
                            tramiteId, "FALTANTE", saldo.abs().toString(),
                            "💸 El ITI debe reembolsar $" + saldo.abs() + " al funcionario.");

                    NotificacionRequestDTO notifDirector = new NotificacionRequestDTO();
                    notifDirector.setTramiteId(tramiteId);
                    notifDirector.setDestinatario(correoDirector);
                    notifDirector.setTipo("Reembolso pendiente de autorización");
                    notifDirector.setMensajePersonalizado(mensaje + "\n\n📌 Escribe: *reembolsos* para ver y autorizar.");
                    notificacionService.guardar(notifDirector);
                    respuesta.append("📬 Se ha notificado al *Director Financiero*.");
                } else {
                    System.err.println("⚠️ No se encontró un Director Financiero en la BD.");
                }
                break;

            case "EXACTO":
                tramiteService.actualizarEstado(tramiteId, "Liquidación cerrada");

                HistorialEstadoRequestDTO historialCierreDTO = new HistorialEstadoRequestDTO();
                historialCierreDTO.setTramiteId(tramiteId);
                historialCierreDTO.setEstadoAnterior("Liquidación aprobada");
                historialCierreDTO.setEstadoNuevo("Liquidación cerrada");
                historialCierreDTO.setUsuarioId(sesion.getUsuarioId());
                historialCierreDTO.setObservacion("Liquidación cerrada automáticamente (caso EXACTO)");
                historialEstadoService.guardar(historialCierreDTO);

                respuesta.append("✅ *Liquidación cerrada automáticamente.*\n");
                respuesta.append("📌 No hay devolución ni reembolso pendiente.\n\n");

                String mensajeCierre = MensajeHelper.liquidacionCerrada(tramiteId, "EXACTO", "0.00");

                if (correoFuncionario != null) {
                    NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
                    notifDTO.setTramiteId(tramiteId);
                    notifDTO.setDestinatario(correoFuncionario);
                    notifDTO.setTipo("Liquidación cerrada");
                    notifDTO.setMensajePersonalizado(mensajeCierre);
                    notificacionService.guardar(notifDTO);
                }

                if (correoDirector != null) {
                    NotificacionRequestDTO notifDirector = new NotificacionRequestDTO();
                    notifDirector.setTramiteId(tramiteId);
                    notifDirector.setDestinatario(correoDirector);
                    notifDirector.setTipo("Liquidación cerrada (caso EXACTO)");
                    notifDirector.setMensajePersonalizado(mensajeCierre);
                    notificacionService.guardar(notifDirector);
                }

                respuesta.append("📬 Se ha notificado al *Funcionario* y al *Director Financiero*.");
                break;

            default:
                respuesta.append("⚠️ Tipo de caso desconocido: ").append(tipoCaso);
        }

        sesion.setTramiteEnRevision(null);
        return respuesta.toString();
    }

    // ============================================================
    // VALIDAR DEVOLUCIONES (SOBRANTE)
    // ============================================================
    private String listarDevolucionesPorValidar() {
        List<DevolucionResponseDTO> devoluciones = devolucionService.listarPorEstado("Comprobante subido");

        if (devoluciones.isEmpty()) {
            return "📋 No hay devoluciones pendientes de validar.\n\n" +
                    "👉 Escribe *menu* para volver al menú principal.";
        }

        StringBuilder lista = new StringBuilder("📋 *Devoluciones pendientes de validar:* (" + devoluciones.size() + ")\n\n");
        for (DevolucionResponseDTO d : devoluciones) {
            lista.append("🆔 Devolución #").append(d.getDevolucionId())
                    .append(" — Trámite #").append(d.getTramiteId())
                    .append(" — $").append(d.getMonto())
                    .append("\n");
        }
        lista.append("\n👉 Escribe *validar devolucion ID* para validarla y cerrar la liquidación.");
        lista.append("\n👉 O escribe *rechazar devolucion ID* si el comprobante no es válido.");
        return lista.toString();
    }

    private String validarDevolucion(Long devolucionId, SesionUsuario sesion) {
        Optional<DevolucionResponseDTO> devolucionOpt = devolucionService.buscarPorId(devolucionId);

        if (devolucionOpt.isEmpty()) {
            return "⚠️ No encontré la devolución #" + devolucionId + ".";
        }

        DevolucionResponseDTO devolucion = devolucionOpt.get();

        if (!"Comprobante subido".equals(devolucion.getEstado())) {
            return "⚠️ La devolución #" + devolucionId + " no está pendiente de validación.\n\n" +
                    "📊 Estado actual: *" + devolucion.getEstado() + "*";
        }

        devolucionService.actualizarEstado(devolucionId, "Validado");

        Long tramiteId = devolucion.getTramiteId();
        tramiteService.actualizarEstado(tramiteId, "Liquidación cerrada");

        HistorialEstadoRequestDTO historialDTO = new HistorialEstadoRequestDTO();
        historialDTO.setTramiteId(tramiteId);
        historialDTO.setEstadoAnterior("Liquidación aprobada");
        historialDTO.setEstadoNuevo("Liquidación cerrada");
        historialDTO.setUsuarioId(sesion.getUsuarioId());
        historialDTO.setObservacion("Devolución validada por la Contadora. Liquidación cerrada.");
        historialEstadoService.guardar(historialDTO);

        String correoFuncionario = tramiteService.obtenerDetalle(tramiteId)
                .map(TramiteDetalleDTO::getCorreoUsuario)
                .orElse(null);
        String correoDirector = usuarioService.obtenerCorreoPorRol("Director Financiero");

        String mensaje = MensajeHelper.devolucionValidada(tramiteId, devolucion.getMonto().toString());

        if (correoFuncionario != null) {
            NotificacionRequestDTO notifFuncionario = new NotificacionRequestDTO();
            notifFuncionario.setTramiteId(tramiteId);
            notifFuncionario.setDestinatario(correoFuncionario);
            notifFuncionario.setTipo("Liquidación cerrada - Devolución validada");
            notifFuncionario.setMensajePersonalizado(mensaje);
            notificacionService.guardar(notifFuncionario);
        }

        if (correoDirector != null) {
            NotificacionRequestDTO notifDirector = new NotificacionRequestDTO();
            notifDirector.setTramiteId(tramiteId);
            notifDirector.setDestinatario(correoDirector);
            notifDirector.setTipo("Liquidación cerrada - Devolución validada");
            notifDirector.setMensajePersonalizado(mensaje);
            notificacionService.guardar(notifDirector);
        }

        return "✅ *Devolución #" + devolucionId + " validada correctamente.*\n\n" +
                "💰 Monto: $" + devolucion.getMonto() + "\n" +
                "🎉 La liquidación del trámite *#" + tramiteId + "* ha sido cerrada.\n\n" +
                "📬 Se ha notificado al *Funcionario* y al *Director Financiero*.";
    }

    private String iniciarRechazoDevolucion(Long devolucionId, SesionUsuario sesion) {
        Optional<DevolucionResponseDTO> devolucionOpt = devolucionService.buscarPorId(devolucionId);

        if (devolucionOpt.isEmpty()) {
            return "⚠️ No encontré la devolución #" + devolucionId + ".";
        }

        DevolucionResponseDTO devolucion = devolucionOpt.get();

        if (!"Comprobante subido".equals(devolucion.getEstado())) {
            return "⚠️ La devolución #" + devolucionId + " no está pendiente de validación.";
        }

        sesion.setTramiteEnRevision(devolucion.getTramiteId());
        sesion.setEsperandoMotivoRechazo(true);

        return "✍️ ¿Cuál es el motivo del rechazo de la devolución #" + devolucionId + "?\n\n" +
                "👉 Escribe el motivo y se enviará al Funcionario.\n" +
                "📌 El Funcionario deberá subir nuevamente el comprobante.";
    }

    // ============================================================
    // RECHAZAR TRÁMITE
    // ============================================================
    private String iniciarRechazo(SesionUsuario sesion) {
        if (sesion.getTramiteEnRevision() == null) {
            return "⚠️ No has seleccionado ningún trámite.";
        }
        sesion.setEsperandoMotivoRechazo(true);
        return "✍️ ¿Cuál es el motivo del rechazo?";
    }

    private String procesarMotivoRechazo(String motivo, SesionUsuario sesion) {
        sesion.setMotivoRechazo(motivo);
        sesion.setEsperandoMotivoRechazo(false);

        Long tramiteId = sesion.getTramiteEnRevision();
        if (tramiteId == null) {
            return "⚠️ No encontré el trámite en revisión.";
        }

        Optional<DevolucionResponseDTO> devolucionOpt = devolucionService.buscarPorTramite(tramiteId);

        if (devolucionOpt.isPresent() && "Comprobante subido".equals(devolucionOpt.get().getEstado())) {
            return rechazarDevolucion(tramiteId, devolucionOpt.get(), motivo, sesion);
        }

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

        String correoFuncionario = tramiteService.obtenerDetalle(tramiteId)
                .map(TramiteDetalleDTO::getCorreoUsuario)
                .orElse(null);

        if (correoFuncionario != null) {
            NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
            notifDTO.setTramiteId(tramiteId);
            notifDTO.setDestinatario(correoFuncionario);
            notifDTO.setTipo("Trámite rechazado por Contadora");
            notificacionService.guardar(notifDTO);
        }

        sesion.setTramiteEnRevision(null);
        sesion.setMotivoRechazo(null);

        return "❌ Trámite *" + tramiteId + "* rechazado.\n\n" +
                "📌 Motivo: " + motivo + "\n\n" +
                "📬 Se ha notificado al *Funcionario*.";
    }

    private String rechazarDevolucion(Long tramiteId, DevolucionResponseDTO devolucion,
                                      String motivo, SesionUsuario sesion) {
        devolucionService.actualizarEstado(devolucion.getDevolucionId(), "Rechazado");

        String correoFuncionario = tramiteService.obtenerDetalle(tramiteId)
                .map(TramiteDetalleDTO::getCorreoUsuario)
                .orElse(null);

        if (correoFuncionario != null) {
            NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
            notifDTO.setTramiteId(tramiteId);
            notifDTO.setDestinatario(correoFuncionario);
            notifDTO.setTipo("Devolución rechazada");
            notificacionService.guardar(notifDTO);
        }

        sesion.setTramiteEnRevision(null);
        sesion.setMotivoRechazo(null);

        return "❌ *Devolución rechazada.*\n\n" +
                "🆔 Devolución: #" + devolucion.getDevolucionId() + "\n" +
                "📌 Motivo: " + motivo + "\n\n" +
                "📬 Se ha notificado al *Funcionario* para que suba nuevamente el comprobante.";
    }

    // ============================================================
    // COMPROBANTES DE TRANSFERENCIA
    // ============================================================
    private String listarComprobantesPorEnviar() {
        List<TramiteResponseDTO> transferidos = tramiteService.listarPorEstado("Transferido");

        if (transferidos.isEmpty()) {
            return "📋 No hay comprobantes pendientes de enviar.\n\n" +
                    "👉 Escribe *menu* para volver al menú principal.";
        }

        StringBuilder lista = new StringBuilder("📋 *Comprobantes por enviar:* (" + transferidos.size() + ")\n\n");
        for (TramiteResponseDTO t : transferidos) {
            lista.append("🆔 Trámite #").append(t.getTramiteId())
                    .append(" — ").append(t.getNombreUsuario()).append("\n");
        }
        lista.append("\n👉 Escribe *enviar comprobante ID* para enviar el comprobante al funcionario.");
        return lista.toString();
    }

    private String enviarComprobante(Long tramiteId, SesionUsuario sesion) {
        String correoFuncionario = tramiteService.obtenerDetalle(tramiteId)
                .map(TramiteDetalleDTO::getCorreoUsuario)
                .orElse(null);

        if (correoFuncionario == null) {
            return "⚠️ No encontré el correo del funcionario del trámite *" + tramiteId + "*.";
        }

        NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
        notifDTO.setTramiteId(tramiteId);
        notifDTO.setDestinatario(correoFuncionario);
        notifDTO.setTipo("Comprobante de transferencia disponible");
        notificacionService.guardar(notifDTO);

        return "✅ Comprobante enviado al funcionario del trámite *" + tramiteId + "*.";
    }
}