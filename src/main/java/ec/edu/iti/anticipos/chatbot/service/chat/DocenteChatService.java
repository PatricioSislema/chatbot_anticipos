package ec.edu.iti.anticipos.chatbot.service.chat;

import ec.edu.iti.anticipos.chatbot.dto.request.FacturaRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.request.HistorialEstadoRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.request.LiquidacionRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.request.NotificacionRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.request.SolicitudRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.request.TramiteRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.*;
import ec.edu.iti.anticipos.chatbot.service.DocumentIntelligenceService;
import ec.edu.iti.anticipos.chatbot.service.FacturaService;
import ec.edu.iti.anticipos.chatbot.service.HistorialEstadoService;
import ec.edu.iti.anticipos.chatbot.service.LiquidacionService;
import ec.edu.iti.anticipos.chatbot.service.NotificacionService;
import ec.edu.iti.anticipos.chatbot.service.SolicitudService;
import ec.edu.iti.anticipos.chatbot.service.TramiteService;
import ec.edu.iti.anticipos.chatbot.sesion.SesionUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ec.edu.iti.anticipos.chatbot.dto.request.DevolucionRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.DevolucionResponseDTO;
import ec.edu.iti.anticipos.chatbot.service.DevolucionService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Servicio especializado en la lógica del Docente.
 * Gestiona solicitudes, liquidaciones, subida de facturas y consultas de estado.
 */
@Service
@RequiredArgsConstructor
public class DocenteChatService {

    private final TramiteService tramiteService;
    private final SolicitudService solicitudService;
    private final HistorialEstadoService historialEstadoService;
    private final NotificacionService notificacionService;
    private final DocumentIntelligenceService documentIntelligenceService;
    private final LiquidacionService liquidacionService;
    private final FacturaService facturaService;
    private final DevolucionService devolucionService;

    // ==================== MENSAJES ====================
    public String procesar(String mensaje, String mensajeLower, SesionUsuario sesion) {

        // LOGS DE DIAGNÓSTICO
        System.out.println("=== DOCENTE PROCESAR ===");
        System.out.println("Mensaje: [" + mensaje + "]");
        System.out.println("AccionPendiente: [" + sesion.getAccionPendiente() + "]");
        System.out.println("SubiendoFacturas: [" + sesion.isSubiendoFacturas() + "]");
        System.out.println("Facturas: [" + sesion.getFacturasExtraidas().size() + "]");

        // 1. SOLICITAR
        if (mensajeLower.contains("solicitar") || mensajeLower.contains("pedir")
                || mensajeLower.contains("anticipo") || mensajeLower.contains("fondos")
                || mensajeLower.contains("plata") || mensajeLower.contains("dinero")
                || mensajeLower.contains("adelanto") || mensajeLower.contains("nueva solicitud")
                || mensajeLower.equals("1")) {
            sesion.setAccionPendiente("SOLICITAR");
            sesion.setSubiendoFacturas(false);
            return "Entendido. Sube el formulario de solicitud en PDF ya llenado y firmado.";
        }

        // 2. LIQUIDAR
        if (mensajeLower.contains("liquidar") || mensajeLower.contains("rendir")
                || mensajeLower.contains("justificar") || mensajeLower.contains("cuadrar")
                || mensajeLower.equals("2")) {
            sesion.setAccionPendiente("LIQUIDAR");
            sesion.setSubiendoFacturas(false);
            return "Entendido. Sube el formulario de liquidación en PDF.";
        }

        // 3. CONSULTAR
        if (mensajeLower.contains("estado") || mensajeLower.contains("seguimiento")
                || mensajeLower.contains("consultar") || mensajeLower.contains("tramite")
                || mensajeLower.contains("trámite") || mensajeLower.equals("3")) {
            return consultarEstado(sesion);
        }

        // 4. AYUDA
        if (mensajeLower.contains("ayuda") || mensajeLower.contains("opciones")
                || mensajeLower.contains("puedo hacer") || mensajeLower.contains("menu")
                || mensajeLower.contains("menú") || mensajeLower.equals("4")) {
            return "Puedo ayudarte con:\n" +
                    "1. Solicitar un anticipo\n" +
                    "2. Liquidar un anticipo\n" +
                    "3. Consultar el estado de tus trámites\n" +
                    "4. Resolver dudas generales";
        }

        // 5. LISTO (terminó de subir facturas)
        if (mensajeLower.contains("listo") && sesion.isSubiendoFacturas()) {
            return calcularYMostrarSaldo(sesion);
        }

        // 6. CONFIRMAR
        if (mensajeLower.contains("sí") || mensajeLower.contains("si") || mensajeLower.contains("confirmo")) {

            if ("SOLICITAR".equals(sesion.getAccionPendiente())
                    && sesion.getFormularioSolicitud() != null) {
                return confirmarSolicitud(sesion);
            }

            if ("LIQUIDAR".equals(sesion.getAccionPendiente())
                    && sesion.getFormularioLiquidacion() != null
                    && !sesion.getFacturasExtraidas().isEmpty()) {
                return confirmarLiquidacion(sesion);
            }
        }

        // 7. DEVOLUCIÓN
        if (mensajeLower.contains("devolucion") || mensajeLower.contains("devolución")) {
            sesion.setAccionPendiente("DEVOLUCION");
            return "Sube el comprobante de depósito de la devolución.";
        }

        return null;
    }

    // ==================== DOCUMENTOS ====================
    public String procesarDocumento(MultipartFile archivo, SesionUsuario sesion) {
        try {

            // VALIDAR que sea un PDF
            String nombreArchivo = archivo.getOriginalFilename();
            String tipoContenido = archivo.getContentType();

            if (nombreArchivo == null || !nombreArchivo.toLowerCase().endsWith(".pdf")) {
                return "⚠️ El archivo debe ser un PDF. Por favor, sube el documento en formato PDF.";
            }

            if (tipoContenido == null || !tipoContenido.contains("pdf")) {
                return "⚠️ El archivo no es un PDF válido. Por favor, vuelve a intentarlo.";
            }
            byte[] bytes = archivo.getBytes();

            // FORMULARIO DE SOLICITUD
            if ("SOLICITAR".equals(sesion.getAccionPendiente())) {
                FormularioSolicitudDTO form = documentIntelligenceService.extraerFormularioSolicitud(bytes);
                if (!form.isExtraccionExitosa()) {
                    return "No se pudo leer el formulario. Verifica que sea el PDF correcto.";
                }

                if (form.getSolicitante() == null && form.getPresupuesto() == null) {
                    return "El PDF no parece ser un formulario de solicitud válido. " +
                            "Asegúrate de subir el formato correcto (FORMULARIO DE ANTICIPO DE FONDOS).";
                }

                sesion.setFormularioSolicitud(form);

                StringBuilder resumen = new StringBuilder();
                resumen.append("He extraído los siguientes datos del formulario:\n");
                resumen.append("Solicitante: ").append(form.getSolicitante()).append("\n");
                resumen.append("Unidad: ").append(form.getUnidad()).append("\n");
                resumen.append("Campus: ").append(form.getCampus()).append("\n");
                resumen.append("Propósito: ").append(form.getProposito()).append("\n");
                resumen.append("Presupuesto: ").append(form.getPresupuesto()).append("\n");
                resumen.append("\n¿Confirmas la solicitud? (sí/no)");
                return resumen.toString();
            }

            // FORMULARIO DE LIQUIDACIÓN
            if ("LIQUIDAR".equals(sesion.getAccionPendiente()) && !sesion.isSubiendoFacturas()) {
                FormularioLiquidacionDTO form = documentIntelligenceService.extraerFormularioLiquidacion(bytes);
                if (!form.isExtraccionExitosa()) {
                    return "No se pudo leer el formulario de liquidación.";
                }

                if (form.getSolicitante() == null && form.getTotalGastos() == null) {
                    return "El PDF no parece ser un formulario de liquidación válido. " +
                            "Asegúrate de subir el formato correcto (FORMATO DE LIQUIDACIÓN DE FONDOS).";
                }

                // Validar que los datos coincidan con la solicitud original
                String errorValidacion = validarFormularioLiquidacion(form, sesion);
                if (errorValidacion != null) {
                    return errorValidacion;
                }

                sesion.setFormularioLiquidacion(form);
                sesion.setSubiendoFacturas(true);

                return "Formulario de liquidación registrado.\n" +
                        "Solicitante: " + form.getSolicitante() + "\n" +
                        "Anticipo: " + form.getAnticipo() + "\n" +
                        "Total gastos: " + form.getTotalGastos() + "\n\n" +
                        "Ahora sube las facturas una por una. Escribe 'listo' cuando termines.";
            }

            // FACTURAS
            if ("LIQUIDAR".equals(sesion.getAccionPendiente()) && sesion.isSubiendoFacturas()) {
                FacturaExtraidaDTO factura = documentIntelligenceService.extraerDatos(bytes);

                if (!factura.isExtraccionExitosa()) {
                    return "No se pudo leer la factura. Verifica que sea el PDF correcto.";
                }

                if (factura.getRuc() == null && factura.getMonto() == null) {
                    return "El PDF no parece ser una factura válida. " +
                            "Asegúrate de subir una factura con RUC y monto.";
                }

                FormularioLiquidacionDTO.LineaFactura linea = new FormularioLiquidacionDTO.LineaFactura();
                linea.setNumeroFactura(factura.getNumeroFactura());
                linea.setProveedor(factura.getProveedor());
                linea.setValor(factura.getMonto());
                linea.setFecha(factura.getFecha());

                sesion.getFacturasExtraidas().add(linea);

                return "Factura registrada:\n" +
                        "Proveedor: " + factura.getProveedor() + "\n" +
                        "Monto: " + factura.getMonto() + "\n" +
                        "Total facturas: " + sesion.getFacturasExtraidas().size() + "\n\n" +
                        "Sube otra factura o escribe 'listo' para continuar.";
            }

            // COMPROBANTE DE DEVOLUCIÓN
            // COMPROBANTE DE DEVOLUCIÓN
            if ("DEVOLUCION".equals(sesion.getAccionPendiente())) {
                // Buscar la devolución pendiente del docente
                Optional<DevolucionResponseDTO> devolucionOpt = devolucionService.buscarPendientePorUsuario(sesion.getUsuarioId());

                if (devolucionOpt.isEmpty()) {
                    return "No tienes devoluciones pendientes.";
                }

                DevolucionResponseDTO devolucion = devolucionOpt.get();

                // ACTUALIZAR la devolución existente (no crear una nueva)
                DevolucionRequestDTO devolucionDTO = new DevolucionRequestDTO();
                devolucionDTO.setLiquidacionId(devolucion.getLiquidacionId());
                devolucionDTO.setMonto(devolucion.getMonto());
                devolucionDTO.setFechaDevolucion(LocalDate.now());
                devolucionDTO.setComprobantePdf(archivo.getOriginalFilename());

                // Actualizar por ID
                devolucionService.actualizar(devolucion.getDevolucionId(), devolucionDTO);

                // Cambiar estado a "Liquidación cerrada"
                tramiteService.actualizarEstado(devolucion.getTramiteId(), "Liquidación cerrada");

                // Historial
                HistorialEstadoRequestDTO historialDTO = new HistorialEstadoRequestDTO();
                historialDTO.setTramiteId(devolucion.getTramiteId());
                historialDTO.setEstadoAnterior("Liquidación aprobada");
                historialDTO.setEstadoNuevo("Liquidación cerrada");
                historialDTO.setUsuarioId(sesion.getUsuarioId());
                historialDTO.setObservacion("Devolución registrada por el docente");
                historialEstadoService.guardar(historialDTO);

                sesion.setAccionPendiente(null);
                return "Devolución registrada. La liquidación ha sido cerrada.";
            }

            return "Acción no reconocida.";

        } catch (Exception e) {
            return "Error al procesar el documento: " + e.getMessage();
        }
    }

    // ==================== VALIDACIÓN ====================
    private String validarFormularioLiquidacion(FormularioLiquidacionDTO form, SesionUsuario sesion) {

        Optional<SolicitudResponseDTO> solicitudOpt =
                solicitudService.buscarAnticipoTransferido(sesion.getUsuarioId());

        if (solicitudOpt.isEmpty()) {
            return "⚠️ No se encontró una solicitud de anticipo transferida para este usuario.";
        }

        SolicitudResponseDTO solicitud = solicitudOpt.get();

        if (form.getSolicitante() != null && sesion.getNombre() != null
                && !form.getSolicitante().equalsIgnoreCase(sesion.getNombre())) {
            return "⚠️ El nombre del solicitante no coincide con el registrado.";
        }

        if (form.getAnticipo() != null && solicitud.getPresupuesto() != null
                && form.getAnticipo().compareTo(solicitud.getPresupuesto()) != 0) {
            return "⚠️ El anticipo del formulario ($" + form.getAnticipo() +
                    ") no coincide con el registrado ($" + solicitud.getPresupuesto() + ").";
        }

        if (form.getUnidad() != null && solicitud.getUnidad() != null
                && !form.getUnidad().equalsIgnoreCase(solicitud.getUnidad())) {
            return "⚠️ La unidad del formulario no coincide con la registrada.";
        }

        if (form.getCampus() != null && solicitud.getCampus() != null
                && !form.getCampus().equalsIgnoreCase(solicitud.getCampus())) {
            return "⚠️ El campus del formulario no coincide con el registrado.";
        }

        if (form.getProposito() != null && solicitud.getProposito() != null
                && !form.getProposito().equalsIgnoreCase(solicitud.getProposito())) {
            return "⚠️ El propósito del formulario no coincide con el registrado.";
        }

        return null;
    }

    // ==================== CÁLCULO DE SALDO ====================
    private String calcularYMostrarSaldo(SesionUsuario sesion) {
        FormularioLiquidacionDTO form = sesion.getFormularioLiquidacion();

        Optional<SolicitudResponseDTO> solicitudOpt = solicitudService.buscarAnticipoTransferido(sesion.getUsuarioId());
        BigDecimal anticipo = solicitudOpt.map(SolicitudResponseDTO::getPresupuesto)
                .orElse(BigDecimal.ZERO);

        BigDecimal totalFacturas = BigDecimal.ZERO;
        for (FormularioLiquidacionDTO.LineaFactura f : sesion.getFacturasExtraidas()) {
            if (f.getValor() != null) {
                totalFacturas = totalFacturas.add(f.getValor());
            }
        }

        BigDecimal saldo = anticipo.subtract(totalFacturas);

        StringBuilder resumen = new StringBuilder();
        resumen.append("Resumen de la liquidación:\n");
        resumen.append("Anticipo: ").append(anticipo).append("\n");
        resumen.append("Total gastado: ").append(totalFacturas).append("\n");
        resumen.append("Saldo: ").append(saldo).append("\n\n");

        if (saldo.compareTo(BigDecimal.ZERO) > 0) {
            resumen.append("Debes devolver ").append(saldo).append(" al ITI.\n");
        } else if (saldo.compareTo(BigDecimal.ZERO) < 0) {
            resumen.append("El ITI te debe devolver ").append(saldo.abs()).append(".\n");
        } else {
            resumen.append("No hay devolución.\n");
        }

        resumen.append("\n¿Confirmas la liquidación? (sí/no)");
        return resumen.toString();
    }

    // ==================== CONFIRMACIONES ====================
    private String confirmarSolicitud(SesionUsuario sesion) {
        FormularioSolicitudDTO form = sesion.getFormularioSolicitud();

        TramiteRequestDTO tramiteDTO = new TramiteRequestDTO();
        tramiteDTO.setUsuarioId(sesion.getUsuarioId());
        tramiteDTO.setTipoTramiteId(1L);
        tramiteDTO.setEstado("Pendiente");
        TramiteResponseDTO tramite = tramiteService.guardar(tramiteDTO);

        SolicitudRequestDTO solicitudDTO = new SolicitudRequestDTO();
        solicitudDTO.setTramiteId(tramite.getTramiteId());
        solicitudDTO.setUnidad(form.getUnidad());
        solicitudDTO.setCampus(form.getCampus());
        solicitudDTO.setProposito(form.getProposito());
        solicitudDTO.setLugar(form.getLugar());
        solicitudDTO.setFechaRealizacion(form.getFechaRealizacion());
        solicitudDTO.setPresupuesto(form.getPresupuesto());
        solicitudService.guardar(solicitudDTO);

        HistorialEstadoRequestDTO historialDTO = new HistorialEstadoRequestDTO();
        historialDTO.setTramiteId(tramite.getTramiteId());
        historialDTO.setEstadoAnterior(null);
        historialDTO.setEstadoNuevo("Pendiente");
        historialDTO.setUsuarioId(sesion.getUsuarioId());
        historialDTO.setObservacion("Solicitud creada por el docente");
        historialEstadoService.guardar(historialDTO);

        NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
        notifDTO.setTramiteId(tramite.getTramiteId());
        notifDTO.setDestinatario("maria.lopez@iti.edu.ec");
        notifDTO.setTipo("Nueva solicitud pendiente");
        notificacionService.guardar(notifDTO);

        sesion.setAccionPendiente(null);
        sesion.setFormularioSolicitud(null);

        return "Solicitud creada con éxito. Trámite ID: " + tramite.getTramiteId() +
                ". Se ha notificado al Jefe Inmediato para su revisión.";
    }

    private String confirmarLiquidacion(SesionUsuario sesion) {
        FormularioLiquidacionDTO form = sesion.getFormularioLiquidacion();

        Optional<SolicitudResponseDTO> solicitudOpt = solicitudService.buscarAnticipoTransferido(sesion.getUsuarioId());
        BigDecimal anticipo = solicitudOpt.map(SolicitudResponseDTO::getPresupuesto)
                .orElse(BigDecimal.ZERO);

        BigDecimal totalFacturas = BigDecimal.ZERO;
        for (FormularioLiquidacionDTO.LineaFactura f : sesion.getFacturasExtraidas()) {
            if (f.getValor() != null) {
                totalFacturas = totalFacturas.add(f.getValor());
            }
        }

        BigDecimal saldo = anticipo.subtract(totalFacturas);

        TramiteRequestDTO tramiteDTO = new TramiteRequestDTO();
        tramiteDTO.setUsuarioId(sesion.getUsuarioId());
        tramiteDTO.setTipoTramiteId(2L);
        tramiteDTO.setEstado("Liquidación en proceso");
        TramiteResponseDTO tramite = tramiteService.guardar(tramiteDTO);

        LiquidacionRequestDTO liquidacionDTO = new LiquidacionRequestDTO();
        liquidacionDTO.setTramiteId(tramite.getTramiteId());
        liquidacionDTO.setTotalGastado(totalFacturas);
        liquidacionDTO.setSaldo(saldo);
        liquidacionDTO.setFechaPresentacion(LocalDate.now());
        LiquidacionResponseDTO liquidacion = liquidacionService.guardar(liquidacionDTO);

        for (FormularioLiquidacionDTO.LineaFactura linea : sesion.getFacturasExtraidas()) {
            FacturaRequestDTO facturaDTO = new FacturaRequestDTO();
            facturaDTO.setLiquidacionId(liquidacion.getLiquidacionId());
            facturaDTO.setNumeroFactura(linea.getNumeroFactura());
            facturaDTO.setProveedor(linea.getProveedor());
            facturaDTO.setFecha(linea.getFecha());
            facturaDTO.setMonto(linea.getValor());
            facturaService.guardar(facturaDTO);
        }

        HistorialEstadoRequestDTO historialDTO = new HistorialEstadoRequestDTO();
        historialDTO.setTramiteId(tramite.getTramiteId());
        historialDTO.setEstadoAnterior(null);
        historialDTO.setEstadoNuevo("Liquidación en proceso");
        historialDTO.setUsuarioId(sesion.getUsuarioId());
        historialDTO.setObservacion("Liquidación creada por el docente");
        historialEstadoService.guardar(historialDTO);

        NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
        notifDTO.setTramiteId(tramite.getTramiteId());
        notifDTO.setDestinatario("sofia.vega@iti.edu.ec");
        notifDTO.setTipo("Nueva liquidación pendiente");
        notificacionService.guardar(notifDTO);

        sesion.setAccionPendiente(null);
        sesion.setFormularioLiquidacion(null);
        sesion.setSubiendoFacturas(false);
        sesion.getFacturasExtraidas().clear();

        return "Liquidación registrada con éxito. Trámite ID: " + tramite.getTramiteId() +
                ". Total gastado: " + totalFacturas + ". Saldo: " + saldo +
                ". Se ha notificado al Asistente Contable.";
    }

    // ==================== CONSULTA ====================
    private String consultarEstado(SesionUsuario sesion) {
        List<TramiteResponseDTO> tramites = tramiteService.listarPorUsuario(sesion.getUsuarioId());

        if (tramites.isEmpty()) {
            return "No tienes trámites registrados.";
        }

        StringBuilder lista = new StringBuilder("Tus trámites:\n");
        for (TramiteResponseDTO t : tramites) {
            lista.append("- Trámite ID: ").append(t.getTramiteId())
                    .append(" | Estado: ").append(t.getEstado())
                    .append("\n");
        }
        return lista.toString();
    }
}