package ec.edu.iti.anticipos.chatbot.dto.request;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para recibir datos de una devolución desde el cliente.
 * Incluye el estado y los campos de auditoría del comprobante.
 */
@Data
public class DevolucionRequestDTO {
    private Long liquidacionId;
    private BigDecimal monto;
    private LocalDate fechaDevolucion;
    private String comprobantePdf;
    private String estado;              // ✅ NUEVO

    // Campos de auditoría del comprobante
    private String bancoOrigen;
    private String cuentaOrigen;
    private BigDecimal montoDepositado;
    private LocalDate fechaDeposito;
}