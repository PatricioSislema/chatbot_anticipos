package ec.edu.iti.anticipos.chatbot.dto.request;

import lombok.Data;

/**
 * DTO para recibir datos de un usuario desde el cliente.
 */
@Data
public class UsuarioRequestDTO {
    private String cedula;
    private String nombre;
    private String correo;
    private String contrasena;
    private Long rolId;
}