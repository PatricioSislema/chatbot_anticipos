package ec.edu.iti.anticipos.chatbot.repository;

import ec.edu.iti.anticipos.chatbot.entity.ComprobanteTransferencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para la entidad ComprobanteTransferencia.
 * Proporciona operaciones CRUD sobre la tabla "comprobante_transferencia"
 * y consultas específicas por trámite y tipo.
 */
@Repository
public interface ComprobanteTransferenciaRepository extends JpaRepository<ComprobanteTransferencia, Long> {

    // Buscar todos los comprobantes de un trámite
    List<ComprobanteTransferencia> findByTramiteTramiteId(Long tramiteId);

    // Buscar comprobantes por tipo (ANTICIPO, DEVOLUCION, REEMBOLSO)
    List<ComprobanteTransferencia> findByTipo(String tipo);

    // Buscar comprobantes de un trámite por tipo específico
    List<ComprobanteTransferencia> findByTramiteTramiteIdAndTipo(Long tramiteId, String tipo);
}