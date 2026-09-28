package ec.edu.iti.anticipos.chatbot.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para devolver los datos extraídos de una factura PDF.
 */
@Data
public class FacturaExtraidaDTO {
    private String numeroFactura;
    private String proveedor;
    private String ruc;
    private LocalDate fecha;
    private String concepto;
    private BigDecimal monto;
    private boolean extraccionExitosa;
}