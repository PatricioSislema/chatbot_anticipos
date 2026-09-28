package ec.edu.iti.anticipos.chatbot.sesion;

import ec.edu.iti.anticipos.chatbot.dto.response.FormularioLiquidacionDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.FormularioSolicitudDTO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    // Indica si el docente está subiendo facturas (después del formulario de liquidación)
    private boolean subiendoFacturas;

    // Facturas extraídas durante la liquidación
    private List<FormularioLiquidacionDTO.LineaFactura> facturasExtraidas = new ArrayList<>();

    // Última vez que el usuario interactuó (para controlar el timeout)
    private LocalDateTime ultimaActividad;

    public SesionUsuario() {
        this.autenticado = false;
        this.esperandoContrasena = false;
        this.ultimaActividad = LocalDateTime.now();
    }
}