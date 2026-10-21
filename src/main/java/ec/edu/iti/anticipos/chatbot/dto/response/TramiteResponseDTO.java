package ec.edu.iti.anticipos.chatbot.dto.response;

import lombok.Data;
import java.time.LocalDate;
import java.math.BigDecimal;

@Data
public class TramiteResponseDTO {
    private Long tramiteId;
    private String estado;
    private LocalDate fechaCreacion;
    private String nombreUsuario;
    private String nombreTipoTramite;
    private String proposito;

    // NUEVOS CAMPOS
    private String correoUsuario;
    private String telefono;
    private BigDecimal presupuesto;
    private LocalDate fechaTransferencia;
}