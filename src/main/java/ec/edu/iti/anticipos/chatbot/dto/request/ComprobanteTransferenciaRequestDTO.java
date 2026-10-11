package ec.edu.iti.anticipos.chatbot.dto.request;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para recibir datos de un comprobante de transferencia desde el cliente.
 * Tipos: ANTICIPO, DEVOLUCION, REEMBOLSO.
 */
@Data
public class ComprobanteTransferenciaRequestDTO {
    private Long tramiteId;
    private String tipo;
    private String archivoPdf;
    private BigDecimal monto;
    private LocalDate fecha;
}