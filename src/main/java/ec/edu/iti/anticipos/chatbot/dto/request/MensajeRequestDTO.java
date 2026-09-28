package ec.edu.iti.anticipos.chatbot.dto.request;

import lombok.Data;

/**
 * DTO para recibir los mensajes del usuario a través del chatbot.
 */
@Data
public class MensajeRequestDTO {
    private String texto;
    private Long usuarioId;
    private String canal;
}
