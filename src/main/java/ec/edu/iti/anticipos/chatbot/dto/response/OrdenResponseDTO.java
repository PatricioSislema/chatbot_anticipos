package ec.edu.iti.anticipos.chatbot.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class OrdenResponseDTO {
    private Long ordenId;
    private Long tramiteId;
    private String nombreFuncionario;
    private Long colectoraId;
    private Long directorId;
    private String numeroCuenta;
    private BigDecimal valor;
    private String concepto;
    private LocalDate fechaGeneracion;
    private LocalDate fechaAprobacion;
    private String estado;
}
