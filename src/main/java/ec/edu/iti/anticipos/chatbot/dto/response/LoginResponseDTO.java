package ec.edu.iti.anticipos.chatbot.dto.response;

import lombok.Data;

/**
 * DTO para devolver la respuesta del inicio de sesión.
 */
@Data
public class LoginResponseDTO {
    private Long usuarioId;
    private String nombre;
    private String correo;
    private String rolNombre;
    private boolean autenticado;
}
