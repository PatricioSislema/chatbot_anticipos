package ec.edu.iti.anticipos.chatbot.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Esta entidad representa la tabla "devolucion".
 * Almacena el comprobante de devolución cuando el funcionario devuelve el excedente.
 * Incluye el estado del proceso: Pendiente, Verificado o Rechazado.
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

    /**
     * Estado de la devolución:
     *  - Pendiente: aún no se ha registrado el comprobante.
     *  - Verificado: la Contadora verificó el comprobante.
     *  - Rechazado: el comprobante no es válido.
     */
    @Column(name = "estado", length = 100)
    private String estado = "Pendiente";

    // ====== NUEVOS campos de auditoría del comprobante ======

    @Column(name = "banco_origen", length = 100)
    private String bancoOrigen;

    @Column(name = "cuenta_origen", length = 30)
    private String cuentaOrigen;

    @Column(name = "monto_depositado", precision = 10, scale = 2)
    private BigDecimal montoDepositado;

    @Column(name = "fecha_deposito")
    private LocalDate fechaDeposito;

    // =======================================================

    // Relación: una devolución pertenece a una sola liquidación
    @OneToOne
    @JoinColumn(name = "liquidacion_id", nullable = false, unique = true)
    private Liquidacion liquidacion;
}