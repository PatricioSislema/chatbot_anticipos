package ec.edu.iti.anticipos.chatbot.dto.request;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para recibir datos de una liquidación desde el cliente.
 */
@Data
public class LiquidacionRequestDTO {
    private Long tramiteId;
    private BigDecimal totalGastado;
    private BigDecimal saldo;
    private LocalDate fechaPresentacion;
}