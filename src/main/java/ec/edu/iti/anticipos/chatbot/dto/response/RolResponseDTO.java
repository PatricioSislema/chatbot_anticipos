package ec.edu.iti.anticipos.chatbot.dto.response;

import lombok.Data;

/**
 * DTO para devolver datos de un rol al cliente.
 */
@Data

public class RolResponseDTO {
    private Long id;
    private String nombre;
    private String descripcion;
}
