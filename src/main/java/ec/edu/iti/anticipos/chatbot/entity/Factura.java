package ec.edu.iti.anticipos.chatbot.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Esta entidad representa la tabla "factura".
 * Almacena las facturas subidas por el docente durante la liquidación.
 */
@Data
@Entity
@Table(name = "factura")
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "factura_id")
    private Long facturaId;

    @Column(name = "numero_factura", length = 20)
    private String numeroFactura;

    @Column(name = "proveedor", length = 100)
    private String proveedor;

    @Column(name = "ruc", length = 13)
    private String ruc;

    @Column(name = "fecha")
    private LocalDate fecha;

    @Column(name = "concepto", length = 200)
    private String concepto;

    @Column(name = "monto", precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "archivo_pdf", length = 255)
    private String archivoPdf;

    // Relación: muchas facturas pertenecen a una sola liquidación
    @ManyToOne
    @JoinColumn(name = "liquidacion_id", nullable = false)
    private Liquidacion liquidacion;
}
