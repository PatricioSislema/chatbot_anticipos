package ec.edu.iti.anticipos.chatbot.dto.response;

import lombok.Data;

/**
 * DTO para devolver la respuesta del inicio de sesión.
 * Incluye el estado de bloqueo y los intentos restantes.
 */
@Data
public class LoginResponseDTO {
    private Long usuarioId;
    private String nombre;
    private String correo;
    private String rolNombre;
    private boolean autenticado;

    // Estado de bloqueo
    private boolean bloqueado;
    private int intentosRestantes;
}
