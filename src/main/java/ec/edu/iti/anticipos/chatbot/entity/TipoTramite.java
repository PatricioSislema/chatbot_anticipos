package ec.edu.iti.anticipos.chatbot.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Esta entidad representa la tabla "tipo_tramite".
 * Almacena los tipos de trámite: Solicitud de Anticipo y Liquidación de Anticipo.
 */
@Data
@Entity
@Table(name = "tipo_tramite")
public class TipoTramite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tipo_tramite_id")
    private Long tipoTramiteId;

    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre;
}