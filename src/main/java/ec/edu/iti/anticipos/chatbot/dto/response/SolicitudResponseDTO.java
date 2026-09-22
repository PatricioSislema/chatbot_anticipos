package ec.edu.iti.anticipos.chatbot.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para devolver datos de una solicitud de anticipo al cliente.
 */
@Data
public class SolicitudResponseDTO {
    private Long solicitudId;
    private Long tramiteId;
    private String unidad;
    private String campus;
    private String proposito;
    private String lugar;
    private LocalDate fechaRealizacion;
    private BigDecimal presupuesto;
    private String firma;
}