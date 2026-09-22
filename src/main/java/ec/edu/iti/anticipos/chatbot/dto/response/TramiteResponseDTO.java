package ec.edu.iti.anticipos.chatbot.dto.response;

import lombok.Data;
import java.time.LocalDate;

/**
 * DTO para devolver datos de un trámite al cliente.
 */
@Data
public class TramiteResponseDTO {
    private Long tramiteId;
    private String estado;
    private LocalDate fechaCreacion;
    private String nombreUsuario;
    private String nombreTipoTramite;
}