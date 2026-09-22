package ec.edu.iti.anticipos.chatbot.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para devolver datos de una liquidación al cliente.
 */
@Data
public class LiquidacionResponseDTO {
    private Long liquidacionId;
    private Long tramiteId;
    private BigDecimal totalGastado;
    private BigDecimal saldo;
    private LocalDate fechaPresentacion;
}
