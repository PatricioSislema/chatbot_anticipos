package ec.edu.iti.anticipos.chatbot.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Esta entidad representa la tabla "orden_transferencia".
 * Almacena la orden de transferencia bancaria que la Colectora genera
 * y el Director Financiero aprueba.
 *
 * Flujo:
 *  1. La Colectora genera la orden con estado 'Generada'.
 *  2. El Director Financiero la revisa en el sistema del banco.
 *  3. Si el Director aprueba, el estado pasa a 'Aprobada' y el dinero se transfiere.
 *  4. El chatbot NO interactúa con el sistema del banco, solo registra el estado.
 */
@Data
@Entity
@Table(name = "orden_transferencia")
public class OrdenTransferencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "orden_id")
    private Long ordenId;

    @Column(name = "numero_cuenta", length = 30)
    private String numeroCuenta;

    @Column(name = "valor", precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "concepto", length = 200)
    private String concepto;

    @Column(name = "fecha_generacion")
    private LocalDate fechaGeneracion;

    @Column(name = "fecha_aprobacion")
    private LocalDate fechaAprobacion;

    /**
     * Estado de la orden:
     *  - Generada: la Colectora creó la orden, esperando aprobación del Director.
     *  - Aprobada: el Director aprobó la orden.
     *  - Rechazada: el Director rechazó la orden.
     */
    @Column(name = "estado", length = 20)
    private String estado = "Generada";

    // Relación: una orden pertenece a un solo trámite
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tramite_id", nullable = false)
    private Tramite tramite;

    // Relación: una orden es generada por una Colectora
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "colectora_id", nullable = false)
    private Usuario colectora;

    // Relación: una orden es aprobada por un Director Financiero (puede ser null hasta que se apruebe)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "director_id")
    private Usuario director;
}