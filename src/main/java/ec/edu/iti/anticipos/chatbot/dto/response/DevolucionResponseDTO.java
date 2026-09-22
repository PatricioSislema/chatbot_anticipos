package ec.edu.iti.anticipos.chatbot.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para devolver datos de una devolución al cliente.
 */
@Data
public class DevolucionResponseDTO {
    private Long devolucionId;
    private Long liquidacionId;
    private BigDecimal monto;
    private LocalDate fechaDevolucion;
    private String comprobantePdf;
}
