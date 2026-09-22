package ec.edu.iti.anticipos.chatbot.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Esta entidad representa la tabla "solicitud".
 * Almacena los datos específicos de una solicitud de anticipo.
 */
@Data
@Entity
@Table(name = "solicitud")
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "solicitud_id")
    private Long solicitudId;

    @Column(name = "unidad", length = 100)
    private String unidad;

    @Column(name = "campus", length = 50)
    private String campus;

    @Column(name = "proposito", length = 200)
    private String proposito;

    @Column(name = "lugar", length = 100)
    private String lugar;

    @Column(name = "fecha_realizacion")
    private LocalDate fechaRealizacion;

    @Column(name = "presupuesto", precision = 10, scale = 2)
    private BigDecimal presupuesto;

    @Column(name = "firma", length = 255)
    private String firma;

    // Relación: una solicitud pertenece a un solo trámite
    @OneToOne
    @JoinColumn(name = "tramite_id", nullable = false, unique = true)
    private Tramite tramite;
}
