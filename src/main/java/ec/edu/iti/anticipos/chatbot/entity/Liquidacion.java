package ec.edu.iti.anticipos.chatbot.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Esta entidad representa la tabla "liquidacion".
 * Almacena los datos de la liquidación de un anticipo: total gastado, saldo y fecha.
 */
@Data
@Entity
@Table(name = "liquidacion")
public class Liquidacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "liquidacion_id")
    private Long liquidacionId;

    @Column(name = "total_gastado", precision = 10, scale = 2)
    private BigDecimal totalGastado;

    @Column(name = "saldo", precision = 10, scale = 2)
    private BigDecimal saldo;

    @Column(name = "fecha_presentacion")
    private LocalDate fechaPresentacion;

    // Relación: una liquidación pertenece a un solo trámite
    @OneToOne
    @JoinColumn(name = "tramite_id", nullable = false, unique = true)
    private Tramite tramite;
}
