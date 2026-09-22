package ec.edu.iti.anticipos.chatbot.dto.response;

import lombok.Data;
import java.time.LocalDate;

/**
 * DTO para devolver datos de un cambio de estado al cliente.
 */
@Data
public class HistorialEstadoResponseDTO {
    private Long historialId;
    private Long tramiteId;
    private String estadoAnterior;
    private String estadoNuevo;
    private String nombreUsuario;
    private LocalDate fechaCambio;
    private String observacion;
}
