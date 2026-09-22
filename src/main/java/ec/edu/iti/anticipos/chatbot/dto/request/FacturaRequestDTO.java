package ec.edu.iti.anticipos.chatbot.dto.request;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para recibir datos de una factura desde el cliente.
 */
@Data
public class FacturaRequestDTO {
    private Long liquidacionId;
    private String numeroFactura;
    private String proveedor;
    private String ruc;
    private LocalDate fecha;
    private String concepto;
    private BigDecimal monto;
    private String archivoPdf;
}