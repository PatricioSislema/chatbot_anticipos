package ec.edu.iti.anticipos.chatbot.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

/**
 * Esta entidad representa la tabla "notificacion".
 * Almacena las notificaciones enviadas a los actores del proceso.
 * Incluye el canal por el cual se envió: Correo, Telegram o Ambos.
 */
@Data
@Entity
@Table(name = "notificacion")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notificacion_id")
    private Long notificacionId;

    @Column(name = "destinatario", length = 100)
    private String destinatario;

    @Column(name = "tipo", length = 100)
    private String tipo;

    @Column(name = "fecha_envio")
    private LocalDate fechaEnvio;

    @Column(name = "estado", length = 20)
    private String estado;

    /**
     * Canal por el cual se envió la notificación:
     *  - Correo: solo por correo electrónico.
     *  - Telegram: solo por el bot de Telegram.
     *  - Ambos: por ambos canales.
     */
    @Column(name = "canal", length = 20)
    private String canal = "Correo";

    // Relación: muchas notificaciones pertenecen a un solo trámite
    @ManyToOne
    @JoinColumn(name = "tramite_id", nullable = false)
    private Tramite tramite;
}