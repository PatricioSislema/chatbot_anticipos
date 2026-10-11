package ec.edu.iti.anticipos.chatbot.sesion;

import ec.edu.iti.anticipos.chatbot.dto.response.FormularioLiquidacionDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.FormularioSolicitudDTO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

/**
 * Representa la sesión activa de un usuario en el chatbot.
 * Almacena el estado de autenticación, el rol, la última actividad
 * y los formularios extraídos durante el flujo conversacional.
 */
@Data
public class SesionUsuario {

    // Datos del usuario autenticado
    private Long usuarioId;
    private String nombre;
    private String correo;
    private String rolNombre;

    // Estado de autenticación
    private boolean autenticado;
    private boolean esperandoContrasena;

    // Acción que el usuario está ejecutando: "SOLICITAR" o "LIQUIDAR"
    private String accionPendiente;

    // Formularios extraídos
    private FormularioSolicitudDTO formularioSolicitud;
    private FormularioLiquidacionDTO formularioLiquidacion;

    // Trámite que el usuario está revisando actualmente (para aprobar/rechazar)
    private Long tramiteEnRevision;

    // Motivo del rechazo (cuando el usuario rechaza un trámite)
    private String motivoRechazo;

    // Indica si el usuario está esperando escribir el motivo del rechazo
    private boolean esperandoMotivoRechazo;

    // Indica si el funcionario está subiendo facturas (después del formulario de liquidación)
    private boolean subiendoFacturas;

    // Facturas extraídas durante la liquidación
    private List<FormularioLiquidacionDTO.LineaFactura> facturasExtraidas = new ArrayList<>();

    // Última vez que el usuario interactuó (para controlar el timeout)
    private LocalDateTime ultimaActividad;

    // ===== NUEVO (FASE 2) =====
    // Indica si el usuario está confirmando la cancelación de una operación
    private boolean esperandoConfirmacionCancelacion;
    // Indica si el usuario está confirmando el cierre de sesión
    private boolean esperandoConfirmacionCerrarSesion;

    // Indica si el bot está procesando un documento del usuario
    private boolean procesandoDocumento;

    // ✅ NUEVO: Indica si el funcionario está subiendo el comprobante de devolución (SOBRANTE)
    private boolean esperandoComprobanteDevolucion;

    // ✅ NUEVO: Monto que el funcionario debe devolver al ITI (SOBRANTE)
    private BigDecimal montoDevolucionPendiente;

    // ✅ NUEVO: ID de la liquidación creada pero pendiente de comprobante (SOBRANTE)
    private Long liquidacionIdEnProceso;

    // Contador de intentos fallidos de contraseña
    private int intentosFallidos = 0;

    public SesionUsuario() {
        this.autenticado = false;
        this.esperandoContrasena = false;
        this.ultimaActividad = LocalDateTime.now();
    }
}