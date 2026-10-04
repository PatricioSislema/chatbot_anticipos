package ec.edu.iti.anticipos.chatbot.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO con la información completa de un trámite para revisión.
 * Incluye datos del trámite, del usuario, del tipo y de la solicitud/liquidación.
 */
@Data
public class TramiteDetalleDTO {

    // Datos del trámite
    private Long tramiteId;
    private String estado;
    private LocalDate fechaCreacion;
    private String nombreTipoTramite;

    // Datos del usuario
    private Long usuarioId;
    private String nombreUsuario;
    private String correoUsuario;
    private String cedulaUsuario;

    // Datos de la solicitud (solo si es solicitud)
    private String unidad;
    private String campus;
    private String proposito;
    private String lugar;
    private LocalDate fechaRealizacion;
    private BigDecimal presupuesto;

    // Datos de la liquidación (solo si es liquidación)
    private BigDecimal totalGastado;
    private BigDecimal saldo;
    private LocalDate fechaPresentacion;
}
