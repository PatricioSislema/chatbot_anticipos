package ec.edu.iti.anticipos.chatbot.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO con los datos extraídos del formulario de liquidación de fondos.
 */
@Data
public class FormularioLiquidacionDTO {
    private String solicitante;
    private String ci;
    private BigDecimal anticipo;
    private String unidad;
    private String campus;
    private LocalDate fecha;
    private String proposito;
    private String lugar;
    private BigDecimal totalGastos;
    private BigDecimal diferencia;
    private String firma;
    private List<LineaFactura> facturas = new ArrayList<>();
    private boolean extraccionExitosa;

    /**
     * Representa una línea de factura del detalle de gastos.
     */
    @Data
    public static class LineaFactura {
        private LocalDate fecha;
        private String numeroFactura;
        private String proveedor;
        private String concepto;
        private BigDecimal valor;
    }
}