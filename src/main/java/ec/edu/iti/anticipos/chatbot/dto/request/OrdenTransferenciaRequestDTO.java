package ec.edu.iti.anticipos.chatbot.dto.request;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para recibir datos de una orden de transferencia desde el cliente.
 */
@Data
public class OrdenTransferenciaRequestDTO {
    private Long tramiteId;
    private Long colectoraId;
    private Long directorId;
    private String numeroCuenta;
    private BigDecimal valor;
    private String concepto;
    private LocalDate fechaGeneracion;
    private LocalDate fechaAprobacion;
    private String estado;
}