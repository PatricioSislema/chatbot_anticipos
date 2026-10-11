package ec.edu.iti.anticipos.chatbot.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Esta entidad representa la tabla "comprobante_transferencia".
 * Centraliza los comprobantes PDF de las transferencias bancarias
 * relacionadas con un trámite.
 *
 * Tipos de comprobante:
 *  - ANTICIPO: comprobante de la transferencia del anticipo al funcionario.
 *  - DEVOLUCION: comprobante de la devolución del excedente al ITI.
 *  - REEMBOLSO: comprobante del reembolso al funcionario (caso FALTANTE).
 *
 * NOTA DE PROTOTIPO: por ahora solo se guarda el NOMBRE del archivo PDF.
 * En producción se debería usar Azure Blob Storage con nombres únicos (UUID).
 */
@Data
@Entity
@Table(name = "comprobante_transferencia")
public class ComprobanteTransferencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comprobante_id")
    private Long comprobanteId;

    /**
     * Tipo de comprobante:
     *  - ANTICIPO
     *  - DEVOLUCION
     *  - REEMBOLSO
     */
    @Column(name = "tipo", nullable = false, length = 30)
    private String tipo;

    /**
     * Nombre del archivo PDF del comprobante.
     * ⚠️ En prototipo solo se guarda el nombre, no el archivo físico.
     */
    @Column(name = "archivo_pdf", length = 255)
    private String archivoPdf;

    @Column(name = "monto", precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "fecha")
    private LocalDate fecha;

    // Relación: muchos comprobantes pertenecen a un solo trámite
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tramite_id", nullable = false)
    private Tramite tramite;
}