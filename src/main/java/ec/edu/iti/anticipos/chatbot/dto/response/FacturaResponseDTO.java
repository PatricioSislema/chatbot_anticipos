package ec.edu.iti.anticipos.chatbot.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para devolver datos de una factura al cliente.
 */
@Data
public class FacturaResponseDTO {
    private Long facturaId;
    private Long liquidacionId;
    private String numeroFactura;
    private String proveedor;
    private String ruc;
    private LocalDate fecha;
    private String concepto;
    private BigDecimal monto;
    private String archivoPdf;
}
