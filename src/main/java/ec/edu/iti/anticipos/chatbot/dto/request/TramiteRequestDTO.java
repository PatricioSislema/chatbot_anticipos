package ec.edu.iti.anticipos.chatbot.dto.request;

import lombok.Data;

/**
 * DTO para recibir datos de un trámite desde el cliente.
 */
@Data
public class TramiteRequestDTO {
    private Long usuarioId;
    private Long tipoTramiteId;
    private String estado;
}