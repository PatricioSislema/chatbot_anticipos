package ec.edu.iti.anticipos.chatbot.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para devolver datos de un comprobante de transferencia al cliente.
 * Usa IDs (Long) en lugar de objetos completos.
 */
@Data
public class ComprobanteTransferenciaResponseDTO {
    private Long comprobanteId;
    private Long tramiteId;
    private String tipo;
    private String archivoPdf;
    private BigDecimal monto;
    private LocalDate fecha;
}