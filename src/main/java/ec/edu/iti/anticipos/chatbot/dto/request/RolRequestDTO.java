package ec.edu.iti.anticipos.chatbot.dto.request;

import lombok.Data;
/**
 * DTO para recibir datos de un rol desde el cliente.
 */

@Data
public class RolRequestDTO {
    private String nombre;
    private String descripcion;
}
