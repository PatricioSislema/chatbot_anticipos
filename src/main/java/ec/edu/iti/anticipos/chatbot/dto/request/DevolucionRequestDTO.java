package ec.edu.iti.anticipos.chatbot.dto.request;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para recibir datos de una devolución desde el cliente.
 */
@Data
public class DevolucionRequestDTO {
    private Long liquidacionId;
    private BigDecimal monto;
    private LocalDate fechaDevolucion;
    private String comprobantePdf;
}
