package ec.edu.iti.anticipos.chatbot.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para devolver datos de una devolución al cliente.
 * Incluye el estado: Pendiente, Verificado o Rechazado.
 */
@Data
public class DevolucionResponseDTO {
    private Long devolucionId;
    private Long liquidacionId;
    private Long tramiteId;
    private BigDecimal monto;
    private LocalDate fechaDevolucion;
    private String comprobantePdf;
    private String estado;

    // NUEVOS campos de auditoría
    private String bancoOrigen;
    private String cuentaOrigen;
    private BigDecimal montoDepositado;
    private LocalDate fechaDeposito;
}