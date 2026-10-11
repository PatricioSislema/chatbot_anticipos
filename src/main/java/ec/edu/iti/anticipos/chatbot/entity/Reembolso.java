package ec.edu.iti.anticipos.chatbot.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Esta entidad representa la tabla "reembolso".
 * Almacena la solicitud de reembolso cuando el funcionario gastó MÁS
 * de lo anticipado (caso FALTANTE en la liquidación).
 *
 * Flujo:
 *  1. La Contadora crea el reembolso con estado 'Pendiente'.
 *  2. Se notifica al Director Financiero para su autorización.
 *  3. Si el Director aprueba, el estado pasa a 'Autorizado' y se envía a la Colectora.
 *  4. Si rechaza, el estado pasa a 'Rechazado'.
 */
@Data
@Entity
@Table(name = "reembolso")
public class Reembolso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reembolso_id")
    private Long reembolsoId;

    @Column(name = "monto_solicitado", precision = 10, scale = 2)
    private BigDecimal montoSolicitado;

    @Column(name = "fecha_solicitud")
    private LocalDate fechaSolicitud;

    @Column(name = "fecha_autorizacion")
    private LocalDate fechaAutorizacion;

    /**
     * Estado del reembolso:
     *  - Pendiente: esperando autorización del Director.
     *  - Autorizado: el Director aprobó; se envía a la Colectora.
     *  - Rechazado: el Director rechazó el reembolso.
     */
    @Column(name = "estado", length = 20)
    private String estado = "Pendiente";

    @Column(name = "observacion", length = 200)
    private String observacion;

    // Relación: un reembolso pertenece a una sola liquidación
    @OneToOne
    @JoinColumn(name = "liquidacion_id", nullable = false, unique = true)
    private Liquidacion liquidacion;

    // Relación: un reembolso es autorizado por un usuario (Director Financiero)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autorizado_por")
    private Usuario autorizadoPor;
}