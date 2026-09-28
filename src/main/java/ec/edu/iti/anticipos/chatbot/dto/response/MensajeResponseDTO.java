package ec.edu.iti.anticipos.chatbot.dto.response;

import lombok.Data;

/**
 * DTO para devolver la respuesta del chatbot al usuario.
 */
@Data
public class MensajeResponseDTO {
    private String respuesta;
    private boolean exitoso;
}
