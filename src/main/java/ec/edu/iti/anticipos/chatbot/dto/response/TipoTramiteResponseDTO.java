package ec.edu.iti.anticipos.chatbot.dto.response;

import lombok.Data;

/**
 * DTO para devolver datos de un tipo de trámite al cliente.
 */
@Data
public class TipoTramiteResponseDTO {
    private Long tipoTramiteId;
    private String nombre;
}
