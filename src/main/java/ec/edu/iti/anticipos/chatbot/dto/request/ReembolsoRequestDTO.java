package ec.edu.iti.anticipos.chatbot.dto.request;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para recibir datos de un reembolso desde el cliente.
 * Caso FALTANTE: el funcionario gastó más de lo anticipado.
 */
@Data
public class ReembolsoRequestDTO {
    private Long liquidacionId;
    private BigDecimal montoSolicitado;
    private LocalDate fechaSolicitud;
    private Long autorizadoPor;        // ID del Director Financiero
    private LocalDate fechaAutorizacion;
    private String estado;
    private String observacion;
}