package ec.edu.iti.anticipos.chatbot.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para devolver datos de una orden de transferencia al cliente.
 * Usa IDs (Long) en lugar de objetos completos.
 */
@Data
public class OrdenTransferenciaResponseDTO {
    private Long ordenId;
    private Long tramiteId;
    private Long colectoraId;
    private String colectoraNombre;
    private Long directorId;
    private String directorNombre;
    private String numeroCuenta;
    private BigDecimal valor;
    private String concepto;
    private LocalDate fechaGeneracion;
    private LocalDate fechaAprobacion;
    private String estado;
}