package ec.edu.iti.anticipos.chatbot.dto.request;

import lombok.Data;

/**
 * DTO para recibir datos de un tipo de trámite desde el cliente.
 */
@Data
public class TipoTramiteRequestDTO {
    private String nombre;
}
