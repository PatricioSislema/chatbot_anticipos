package ec.edu.iti.anticipos.chatbot.dto.request;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para recibir datos de una solicitud de anticipo desde el cliente.
 */
@Data
public class SolicitudRequestDTO {
    private Long tramiteId;
    private String unidad;
    private String campus;
    private String proposito;
    private String lugar;
    private LocalDate fechaRealizacion;
    private BigDecimal presupuesto;
    private String firma;
}
