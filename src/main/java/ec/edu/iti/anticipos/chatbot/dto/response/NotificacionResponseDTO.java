package ec.edu.iti.anticipos.chatbot.dto.response;

import lombok.Data;
import java.time.LocalDate;

/**
 * DTO para devolver datos de una notificación al cliente.
 */
@Data
public class NotificacionResponseDTO {
    private Long notificacionId;
    private Long tramiteId;
    private String destinatario;
    private String tipo;
    private LocalDate fechaEnvio;
    private String estado;
}
