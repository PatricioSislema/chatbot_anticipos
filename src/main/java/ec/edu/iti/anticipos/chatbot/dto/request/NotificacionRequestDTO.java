package ec.edu.iti.anticipos.chatbot.dto.request;

import lombok.Data;

/**
 * DTO para recibir datos de una notificación desde el cliente.
 */
@Data
public class NotificacionRequestDTO {
    private Long tramiteId;
    private String destinatario;
    private String tipo;
    private String estado;
}