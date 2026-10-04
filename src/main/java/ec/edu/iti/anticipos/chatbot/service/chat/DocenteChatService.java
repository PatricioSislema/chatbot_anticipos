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
import ec.edu.iti.anticipos.chatbot.entity.Tramite;
import ec.edu.iti.anticipos.chatbot.repository.TramiteRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Servicio especializado en la lógica del Docente.
 * Gestiona solicitudes, liquidaciones, subida de facturas y consultas de estado.
 * También se encarga de la validación de los formularios PDF.
 */
@Service
@RequiredArgsConstructor
public class DocenteChatService {

    private final TramiteService tramiteService;
    private final TramiteRepository tramiteRepository;
    private final SolicitudService solicitudService;
    private final HistorialEstadoService historialEstadoService;
    private final NotificacionService notificacionService;
    private final DocumentIntelligenceService documentIntelligenceService;
    private final LiquidacionService liquidacionService;
    private final FacturaService facturaService;
    private final DevolucionService devolucionService;


    // ==================== MENSAJES ====================
    public String procesar(String mensaje, String mensajeLower, SesionUsuario sesion) {

        System.out.println("=== DOCENTE PROCESAR ===");
        System.out.println("Mensaje: [" + mensaje + "]");
        System.out.println("AccionPendiente: [" + sesion.getAccionPendiente() + "]");

        // Si tiene formulario de solicitud subido
        if (sesion.getFormularioSolicitud() != null) {
            if (mensajeLower.equals("si") || mensajeLower.equals("sí") || mensajeLower.equals("1") || mensajeLower.equals("confirmo")) {
                return confirmarSolicitud(sesion);
            }
            if (mensajeLower.equals("no") || mensajeLower.equals("2")) {
                sesion.setFormularioSolicitud(null);
                sesion.setAccionPendiente(null);
                return "✅ Solicitud descartada.\n\n¿Qué necesitas hacer hoy?";
            }
            return null;
        }

        // Si tiene formulario de liquidación subido
        if (sesion.getFormularioLiquidacion() != null && !sesion.isSubiendoFacturas()) {
            if (mensajeLower.equals("si") || mensajeLower.equals("sí") || mensajeLower.equals("1") || mensajeLower.equals("confirmo")) {
                return confirmarLiquidacion(sesion);
            }
            if (mensajeLower.equals("no") || mensajeLower.equals("2")) {
                sesion.setFormularioLiquidacion(null);
                sesion.setAccionPendiente(null);
                return "✅ Liquidación descartada.\n\n¿Qué necesitas hacer hoy?";
            }
            return null;
        }

        // Si está subiendo facturas y ya mostró el resumen (esperando confirmación)
        if (sesion.isSubiendoFacturas() && sesion.getFormularioLiquidacion() != null) {
            if (mensajeLower.equals("si") || mensajeLower.equals("sí") || mensajeLower.equals("1") || mensajeLower.equals("confirmo")) {
                return confirmarLiquidacion(sesion);
            }
            if (mensajeLower.equals("no") || mensajeLower.equals("2")) {
                sesion.setFormularioLiquidacion(null);
                sesion.setAccionPendiente(null);
                sesion.setSubiendoFacturas(false);
                sesion.getFacturasExtraidas().clear();
                return "✅ Liquidación descartada.\n\n¿Qué necesitas hacer hoy?";
            }
            // ✅ Si escribe "listo", mostrar el resumen de saldo
            if (mensajeLower.contains("listo")) {
                return calcularYMostrarSaldo(sesion);
            }
            return null;
        }

        // ============================================
        // COMANDOS NORMALES
        // ============================================

        // 1. SOLICITAR
        if (mensajeLower.contains("solicitar") || mensajeLower.contains("pedir")
                || mensajeLower.contains("anticipo") || mensajeLower.contains("fondos")
                || mensajeLower.contains("plata") || mensajeLower.contains("dinero")
                || mensajeLower.contains("adelanto") || mensajeLower.contains("nueva solicitud")
                || mensajeLower.equals("1")) {

            // ✅ VALIDACIÓN: verificar si tiene liquidaciones pendientes
            if (tieneLiquidacionesPendientes(sesion.getUsuarioId())) {
                return "⚠️ *No puedes solicitar un nuevo anticipo*\n\n" +
                        "📌 Tienes una liquidación pendiente de completar.\n\n" +
                        "👉 Debes terminar el proceso de liquidación actual antes de solicitar un nuevo anticipo.\n\n" +
                        "📋 Escribe *consultar* para ver tus trámites pendientes.";
            }

            sesion.setAccionPendiente("SOLICITAR");
            sesion.setSubiendoFacturas(false);
            return "📎 Perfecto. Ahora adjunta el formulario de solicitud de anticipo en formato PDF, ya llenado y firmado.";
        }

        // 2. LIQUIDAR
        if (mensajeLower.contains("liquidar") || mensajeLower.contains("rendir")
                || mensajeLower.contains("justificar") || mensajeLower.contains("cuadrar")
                || mensajeLower.equals("2")) {
            sesion.setAccionPendiente("LIQUIDAR");
            sesion.setSubiendoFacturas(false);
            return "📎 Perfecto. Ahora adjunta el formulario de liquidación en formato PDF.";
        }

        // 3. CONSULTAR
        if (mensajeLower.contains("estado") || mensajeLower.contains("seguimiento")
                || mensajeLower.contains("consultar") || mensajeLower.contains("tramite")
                || mensajeLower.contains("trámite") || mensajeLower.equals("3")) {
            return consultarEstado(sesion);
        }

        // 4. AYUDA
        if (mensajeLower.contains("ayuda") || mensajeLower.contains("opciones")
                || mensajeLower.contains("puedo hacer") || mensajeLower.equals("4")) {
            return "ℹ️ Puedo ayudarte con:\n\n" +
                    "1. Solicitar un anticipo\n" +
                    "2. Liquidar un anticipo\n" +
                    "3. Consultar el estado de tus trámites\n" +
                    "4. Resolver dudas generales";
        }

        // 5. LISTO (terminó de subir facturas)
        if (mensajeLower.contains("listo") && sesion.isSubiendoFacturas()) {
            return calcularYMostrarSaldo(sesion);
        }

        // 6. DEVOLUCIÓN
        if (mensajeLower.contains("devolucion") || mensajeLower.contains("devolución")) {
            sesion.setAccionPendiente("DEVOLUCION");
            return "📎 Perfecto. Adjunta el comprobante de depósito de la devolución en formato PDF.";
        }

        return null;
    }

    // ==================== DOCUMENTOS ====================
    public String procesarDocumento(MultipartFile archivo, SesionUsuario sesion) {
        try {

            String nombreArchivo = archivo.getOriginalFilename();
            String tipoContenido = archivo.getContentType();

            if (nombreArchivo == null || !nombreArchivo.toLowerCase().endsWith(".pdf")) {
                return "⚠️ El archivo enviado no es un PDF.\n\n" +
                        "📌 En este paso necesito un documento PDF.\n\n" +
                        "👉 Adjúntalo nuevamente con la extensión .pdf";
            }

            if (tipoContenido == null || !tipoContenido.contains("pdf")) {
                return "⚠️ El archivo no parece ser un PDF válido.\n\n" +
                        "📌 En este paso necesito un documento PDF.\n\n" +
                        "👉 Verifica el archivo y vuelve a enviarlo.";
            }

            byte[] bytes = archivo.getBytes();

            // FORMULARIO DE SOLICITUD
            if ("SOLICITAR".equals(sesion.getAccionPendiente())) {
                FormularioSolicitudDTO form = documentIntelligenceService.extraerFormularioSolicitud(bytes);
                if (!form.isExtraccionExitosa()) {
                    return "⚠️ No pude leer el formulario.\n\n" +
                            "📌 El archivo puede estar dañado o protegido.\n\n" +
                            "👉 Verifica que sea el PDF correcto y vuelve a enviarlo.";
                }

                if (form.getSolicitante() == null && form.getPresupuesto() == null) {
                    return "⚠️ El PDF no corresponde al formulario de solicitud de anticipo.\n\n" +
                            "📌 En este paso necesito el formato *FORMULARIO DE ANTICIPO DE FONDOS*.\n\n" +
                            "👉 Verifica el documento y vuelve a adjuntarlo.";
                }

                sesion.setFormularioSolicitud(form);

                StringBuilder resumen = new StringBuilder();
                resumen.append("✅ He procesado el formulario.\n\n");
                resumen.append("Estos son los datos encontrados:\n\n");
                resumen.append("👤 Solicitante: ").append(form.getSolicitante()).append("\n");
                resumen.append("🏢 Unidad: ").append(form.getUnidad()).append("\n");
                resumen.append("📍 Campus: ").append(form.getCampus()).append("\n");
                resumen.append("🎯 Propósito: ").append(form.getProposito()).append("\n");
                resumen.append("💰 Presupuesto: $").append(form.getPresupuesto()).append("\n");
                resumen.append("\n¿Los datos son correctos?");
                return resumen.toString();
            }

            // FORMULARIO DE LIQUIDACIÓN
            if ("LIQUIDAR".equals(sesion.getAccionPendiente()) && !sesion.isSubiendoFacturas()) {
                FormularioLiquidacionDTO form = documentIntelligenceService.extraerFormularioLiquidacion(bytes);
                if (!form.isExtraccionExitosa()) {
                    return "⚠️ No pude leer el formulario de liquidación.\n\n" +
                            "📌 El archivo puede estar dañado o protegido.\n\n" +
                            "👉 Verifica el archivo y vuelve a enviarlo.";
                }

                if (form.getSolicitante() == null && form.getTotalGastos() == null) {
                    return "⚠️ El PDF no corresponde al formulario de liquidación.\n\n" +
                            "📌 En este paso necesito el formato *FORMATO DE LIQUIDACIÓN DE FONDOS*.\n\n" +
                            "👉 Verifica el documento y vuelve a adjuntarlo.";
                }

                String errorValidacion = validarFormularioLiquidacion(form, sesion);
                if (errorValidacion != null) {
                    return errorValidacion;
                }

                sesion.setFormularioLiquidacion(form);
                sesion.setSubiendoFacturas(true);

                return "✅ Formulario de liquidación registrado.\n\n" +
                        "👤 Solicitante: " + form.getSolicitante() + "\n" +
                        "💰 Anticipo: " + form.getAnticipo() + "\n" +
                        "📊 Total gastos: " + form.getTotalGastos() + "\n\n" +
                        "📎 Ahora sube las facturas una por una.\n\n" +
                        "✍️ Escribe *listo* cuando termines.";
            }

            // FACTURAS
            if ("LIQUIDAR".equals(sesion.getAccionPendiente()) && sesion.isSubiendoFacturas()) {
                FacturaExtraidaDTO factura = documentIntelligenceService.extraerDatos(bytes);

                if (!factura.isExtraccionExitosa()) {
                    return "⚠️ No pude leer la factura.\n\n" +
                            "📌 El archivo puede estar dañado o protegido.\n\n" +
                            "👉 Verifica que sea una factura válida y vuelve a enviarla.";
                }

                if (factura.getRuc() == null && factura.getMonto() == null) {
                    return "⚠️ El PDF no corresponde a una factura válida.\n\n" +
                            "📌 En este paso necesito una factura con RUC y monto.\n\n" +
                            "👉 Verifica el archivo y vuelve a enviarlo.";
                }

                FormularioLiquidacionDTO.LineaFactura linea = new FormularioLiquidacionDTO.LineaFactura();
                linea.setNumeroFactura(factura.getNumeroFactura());
                linea.setProveedor(factura.getProveedor());
                linea.setValor(factura.getMonto());
                linea.setFecha(factura.getFecha());

                sesion.getFacturasExtraidas().add(linea);

                return "✅ Factura registrada:\n\n" +
                        "🏢 Proveedor: " + factura.getProveedor() + "\n" +
                        "💰 Monto: $" + factura.getMonto() + "\n" +
                        "📊 Total facturas: " + sesion.getFacturasExtraidas().size() + "\n\n" +
                        "📎 Sube otra factura o escribe *listo* para continuar.";
            }

            // COMPROBANTE DE DEVOLUCIÓN
            if ("DEVOLUCION".equals(sesion.getAccionPendiente())) {
                Optional<DevolucionResponseDTO> devolucionOpt = devolucionService.buscarPendientePorUsuario(sesion.getUsuarioId());

                if (devolucionOpt.isEmpty()) {
                    return "⚠️ No tienes devoluciones pendientes.\n\n" +
                            "📌 No hay ninguna liquidación aprobada con saldo por devolver.\n\n" +
                            "👉 Escribe *menu* para volver al menú principal.";
                }

                DevolucionResponseDTO devolucion = devolucionOpt.get();

                DevolucionRequestDTO devolucionDTO = new DevolucionRequestDTO();
                devolucionDTO.setLiquidacionId(devolucion.getLiquidacionId());
                devolucionDTO.setMonto(devolucion.getMonto());
                devolucionDTO.setFechaDevolucion(LocalDate.now());
                devolucionDTO.setComprobantePdf(archivo.getOriginalFilename());

                devolucionService.actualizar(devolucion.getDevolucionId(), devolucionDTO);

                tramiteService.actualizarEstado(devolucion.getTramiteId(), "Liquidación cerrada");

                HistorialEstadoRequestDTO historialDTO = new HistorialEstadoRequestDTO();
                historialDTO.setTramiteId(devolucion.getTramiteId());
                historialDTO.setEstadoAnterior("Liquidación aprobada");
                historialDTO.setEstadoNuevo("Liquidación cerrada");
                historialDTO.setUsuarioId(sesion.getUsuarioId());
                historialDTO.setObservacion("Devolución registrada por el docente");
                historialEstadoService.guardar(historialDTO);

                sesion.setAccionPendiente(null);
                return "✅ Devolución registrada correctamente.\n\n" +
                        "🎉 La liquidación ha sido cerrada.\n\n" +
                        "Gracias por usar el sistema.";
            }

            return "⚠️ Acción no reconocida.\n\n" +
                    "📌 No sé qué hacer con este documento.\n\n" +
                    "👉 Escribe *menu* para volver al menú principal.";

        } catch (Exception e) {
            return "⚠️ Ocurrió un error al procesar el documento.\n\n" +
                    "📌 Detalle técnico: " + e.getMessage() + "\n\n" +
                    "👉 Intenta nuevamente o escribe *menu* para volver.";
        }
    }

    // ==================== VALIDACIÓN ====================
    private String validarFormularioLiquidacion(FormularioLiquidacionDTO form, SesionUsuario sesion) {

        Optional<SolicitudResponseDTO> solicitudOpt =
                solicitudService.buscarAnticipoTransferido(sesion.getUsuarioId());

        if (solicitudOpt.isEmpty()) {
            return "⚠️ No encontré una solicitud de anticipo transferida para tu usuario.\n\n" +
                    "📌 Para liquidar necesitas tener un anticipo transferido.\n\n" +
                    "👉 Verifica que tu jefe y el director ya hayan aprobado tu solicitud.";
        }

        SolicitudResponseDTO solicitud = solicitudOpt.get();

        if (form.getSolicitante() != null && sesion.getNombre() != null) {
            String solicitanteForm = normalizar(form.getSolicitante());
            String nombreSesion = normalizar(sesion.getNombre());

            if (!solicitanteForm.equals(nombreSesion)) {
                return "⚠️ El nombre del solicitante no coincide con el registrado.\n\n" +
                        "📌 El formulario dice: *" + form.getSolicitante() + "*\n" +
                        "📌 Tu usuario es: *" + sesion.getNombre() + "*\n\n" +
                        "👉 Por seguridad, el formulario debe estar a tu nombre.";
            }
        }

        if (form.getUnidad() != null && solicitud.getUnidad() != null) {
            String unidadForm = normalizar(form.getUnidad());
            String unidadSolicitud = normalizar(solicitud.getUnidad());

            if (!unidadForm.equals(unidadSolicitud)) {
                return "⚠️ La unidad del formulario no coincide con la registrada.\n\n" +
                        "📌 El formulario dice: *" + form.getUnidad() + "*\n" +
                        "📌 Lo registrado fue: *" + solicitud.getUnidad() + "*";
            }
        }

        if (form.getCampus() != null && solicitud.getCampus() != null) {
            String campusForm = normalizar(form.getCampus());
            String campusSolicitud = normalizar(solicitud.getCampus());

            if (!campusForm.equals(campusSolicitud)) {
                return "⚠️ El campus del formulario no coincide con el registrado.\n\n" +
                        "📌 El formulario dice: *" + form.getCampus() + "*\n" +
                        "📌 Lo registrado fue: *" + solicitud.getCampus() + "*";
            }
        }

        if (form.getProposito() != null && solicitud.getProposito() != null) {
            String propositoForm = normalizar(form.getProposito());
            String propositoSolicitud = normalizar(solicitud.getProposito());

            if (!propositoForm.equals(propositoSolicitud)) {
                return "⚠️ El propósito del formulario no coincide con el registrado.\n\n" +
                        "📌 El formulario dice: *" + form.getProposito() + "*\n" +
                        "📌 Lo registrado fue: *" + solicitud.getProposito() + "*";
            }
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
        resumen.append("📊 *Resumen de la liquidación*\n\n");
        resumen.append("💰 Anticipo: $").append(anticipo).append("\n");
        resumen.append("📉 Total gastado: $").append(totalFacturas).append("\n");
        resumen.append("📈 Saldo: $").append(saldo).append("\n\n");

        if (saldo.compareTo(BigDecimal.ZERO) > 0) {
            resumen.append("⚠️ Debes devolver $").append(saldo).append(" al ITI.\n");
        } else if (saldo.compareTo(BigDecimal.ZERO) < 0) {
            resumen.append("ℹ️ El ITI te debe devolver $").append(saldo.abs()).append(".\n");
        } else {
            resumen.append("✅ No hay devolución pendiente.\n");
        }

        resumen.append("\n¿Confirmas la liquidación?");
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

        return "✅ Solicitud registrada correctamente.\n\n" +
                "🆔 Número: *ANT-" + String.format("%05d", tramite.getTramiteId()) + "*\n\n" +
                "📬 Se ha notificado al Jefe Inmediato para su revisión.";
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

        // Crear trámite de liquidación
        TramiteRequestDTO tramiteDTO = new TramiteRequestDTO();
        tramiteDTO.setUsuarioId(sesion.getUsuarioId());
        tramiteDTO.setTipoTramiteId(2L);
        tramiteDTO.setEstado("Liquidación en proceso");
        TramiteResponseDTO tramite = tramiteService.guardar(tramiteDTO);

        // Crear liquidación
        LiquidacionRequestDTO liquidacionDTO = new LiquidacionRequestDTO();
        liquidacionDTO.setTramiteId(tramite.getTramiteId());
        liquidacionDTO.setTotalGastado(totalFacturas);
        liquidacionDTO.setSaldo(saldo);
        liquidacionDTO.setFechaPresentacion(LocalDate.now());
        LiquidacionResponseDTO liquidacion = liquidacionService.guardar(liquidacionDTO);

        // Guardar facturas
        for (FormularioLiquidacionDTO.LineaFactura linea : sesion.getFacturasExtraidas()) {
            FacturaRequestDTO facturaDTO = new FacturaRequestDTO();
            facturaDTO.setLiquidacionId(liquidacion.getLiquidacionId());
            facturaDTO.setNumeroFactura(linea.getNumeroFactura());
            facturaDTO.setProveedor(linea.getProveedor());
            facturaDTO.setFecha(linea.getFecha());
            facturaDTO.setMonto(linea.getValor());
            facturaService.guardar(facturaDTO);
        }

        // Historial
        HistorialEstadoRequestDTO historialDTO = new HistorialEstadoRequestDTO();
        historialDTO.setTramiteId(tramite.getTramiteId());
        historialDTO.setEstadoAnterior(null);
        historialDTO.setEstadoNuevo("Liquidación en proceso");
        historialDTO.setUsuarioId(sesion.getUsuarioId());
        historialDTO.setObservacion("Liquidación creada por el docente");
        historialEstadoService.guardar(historialDTO);

        // Notificar al Asistente Contable
        NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
        notifDTO.setTramiteId(tramite.getTramiteId());
        notifDTO.setDestinatario("sofia.vega@iti.edu.ec");
        notifDTO.setTipo("Nueva liquidación pendiente");
        notificacionService.guardar(notifDTO);

        // Limpiar formulario y facturas
        sesion.setFormularioLiquidacion(null);
        sesion.setSubiendoFacturas(false);
        sesion.getFacturasExtraidas().clear();

        // NO ponemos accionPendiente = DEVOLUCION.
        // La devolución la crea la Contadora al aprobar.
        sesion.setAccionPendiente(null);

        // Mensaje al docente (sin mencionar la devolución todavía)
        StringBuilder respuesta = new StringBuilder();
        respuesta.append("✅ Liquidación registrada correctamente.\n\n");
        respuesta.append("🆔 Trámite: *").append(tramite.getTramiteId()).append("*\n");
        respuesta.append("💰 Total gastado: $").append(totalFacturas).append("\n");
        respuesta.append("📈 Saldo: $").append(saldo).append("\n\n");

        if (saldo.compareTo(BigDecimal.ZERO) > 0) {
            respuesta.append("⚠️ Deberás devolver $").append(saldo).append(" al ITI.\n");
            respuesta.append("(Se te notificará cuando la liquidación sea aprobada).\n\n");
        } else if (saldo.compareTo(BigDecimal.ZERO) < 0) {
            respuesta.append("ℹ️ El ITI te deberá devolver $").append(saldo.abs()).append(".\n");
            respuesta.append("(Se te notificará cuando la liquidación sea aprobada).\n\n");
        } else {
            respuesta.append("✅ No hay devolución pendiente.\n\n");
        }

        respuesta.append("📬 Se ha notificado al Asistente Contable para su revisión.\n\n");
        respuesta.append("👉 Cuando sea aprobada por la Contadora, recibirás una notificación.");

        return respuesta.toString();
    }

    // ==================== CONSULTA ====================
    private String consultarEstado(SesionUsuario sesion) {
        List<TramiteResponseDTO> tramites = tramiteService.listarPorUsuario(sesion.getUsuarioId());

        if (tramites.isEmpty()) {
            return "📋 No tienes trámites registrados aún.\n\n" +
                    "👉 Usa la opción *1* para solicitar un anticipo.";
        }

        StringBuilder lista = new StringBuilder("📋 *Tus trámites:*\n\n");
        for (TramiteResponseDTO t : tramites) {
            lista.append("🆔 ").append(t.getTramiteId())
                    .append(" — ").append(t.getEstado()).append("\n");
        }
        return lista.toString();
    }

    // ==================== UTILIDADES ====================
    private String normalizar(String texto) {
        if (texto == null) return "";
        return java.text.Normalizer
                .normalize(texto, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .trim()
                .replaceAll("\\s+", " ");
    }

    // ============================================================
// VERIFICAR SI TIENE LIQUIDACIONES PENDIENTES
// ============================================================
    private boolean tieneLiquidacionesPendientes(Long usuarioId) {
        try {
            List<String> estadosPendientes = List.of(
                    "Liquidación en proceso",
                    "Liquidación revisada",
                    "Liquidación aprobada",
                    "Devuelto al docente"
            );

            List<Tramite> pendientes = tramiteRepository.buscarPorUsuarioYEstados(usuarioId, estadosPendientes);
            return !pendientes.isEmpty();
        } catch (Exception e) {
            System.err.println("Error al verificar liquidaciones pendientes: " + e.getMessage());
            return false;
        }
    }
}