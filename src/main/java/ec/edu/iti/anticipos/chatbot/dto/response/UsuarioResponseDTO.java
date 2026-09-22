package ec.edu.iti.anticipos.chatbot.dto.response;

import lombok.Data;

/**
 * DTO para devolver datos de un usuario al cliente.
 * No se incluye la contraseña por motivo de seguridad.
 */
@Data
public class UsuarioResponseDTO {
    private Long usuarioId;
    private String cedula;
    private String nombre;
    private String correo;
    private String rolNombre;
}
