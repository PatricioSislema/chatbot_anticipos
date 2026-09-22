package ec.edu.iti.anticipos.chatbot.dto.request;

import lombok.Data;

/**
 * DTO para recibir datos de un cambio de estado desde el cliente.
 */
@Data
public class HistorialEstadoRequestDTO {
    private Long tramiteId;
    private String estadoAnterior;
    private String estadoNuevo;
    private Long usuarioId;
    private String observacion;
}
