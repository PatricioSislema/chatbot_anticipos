package ec.edu.iti.anticipos.chatbot.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * DTO con los datos extraídos del formulario de solicitud de anticipo.
 */
@Data
public class FormularioSolicitudDTO {
    private String solicitante;
    private String unidad;
    private String campus;
    private LocalDate fecha;
    private String proposito;
    private String lugar;
    private LocalDate fechaRealizacion;
    private BigDecimal presupuesto;

    // Rubros del presupuesto (clave: nombre del rubro, valor: monto)
    private Map<String, BigDecimal> rubros = new HashMap<>();

    private BigDecimal total;
    private String firma;
    private String ci;
    private boolean extraccionExitosa;
}