package ec.edu.iti.anticipos.chatbot.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

/**
 * Esta entidad representa la tabla "historial_estado".
 * Almacena cada cambio de estado de un trámite para garantizar la trazabilidad.
 */
@Data
@Entity
@Table(name = "historial_estado")
public class HistorialEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "historial_id")
    private Long historialId;

    @Column(name = "estado_anterior", length = 30)
    private String estadoAnterior;

    @Column(name = "estado_nuevo", length = 30)
    private String estadoNuevo;

    @Column(name = "fecha_cambio")
    private LocalDate fechaCambio;

    @Column(name = "observacion", length = 200)
    private String observacion;

    // Relación: muchos historiales pertenecen a un solo trámite
    @ManyToOne
    @JoinColumn(name = "tramite_id", nullable = false)
    private Tramite tramite;

    // Relación: muchos historiales son generados por un solo usuario
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
}