package ec.edu.iti.anticipos.chatbot.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Esta entidad representa la tabla "devolucion".
 * Almacena el comprobante de devolución cuando el docente devuelve el excedente.
 */
@Data
@Entity
@Table(name = "devolucion")
public class Devolucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "devolucion_id")
    private Long devolucionId;

    @Column(name = "monto", precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "fecha_devolucion")
    private LocalDate fechaDevolucion;

    @Column(name = "comprobante_pdf", length = 255)
    private String comprobantePdf;

    // Relación: una devolución pertenece a una sola liquidación
    @OneToOne
    @JoinColumn(name = "liquidacion_id", nullable = false, unique = true)
    private Liquidacion liquidacion;
}
