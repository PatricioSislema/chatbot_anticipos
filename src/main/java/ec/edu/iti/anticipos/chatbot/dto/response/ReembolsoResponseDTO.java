package ec.edu.iti.anticipos.chatbot.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para devolver datos de un reembolso al cliente.
 * Usa IDs (Long) en lugar de objetos completos.
 */
@Data
public class ReembolsoResponseDTO {
    private Long reembolsoId;
    private Long liquidacionId;
    private Long tramiteId;
    private BigDecimal montoSolicitado;
    private LocalDate fechaSolicitud;
    private Long autorizadoPorId;
    private String autorizadoPorNombre;
    private LocalDate fechaAutorizacion;
    private String estado;
    private String observacion;
}