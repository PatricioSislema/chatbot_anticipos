package ec.edu.iti.anticipos.chatbot.dto.request;

import lombok.Data;

/**
 * DTO para recibir las credenciales de inicio de sesión.
 * El correo se usa como nombre de usuario.
 */
@Data
public class LoginRequestDTO {
    private String correo;
    private String contrasena;
}
