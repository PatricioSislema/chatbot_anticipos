package ec.edu.iti.anticipos.chatbot.repository;

import ec.edu.iti.anticipos.chatbot.entity.OrdenTransferencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad OrdenTransferencia.
 * Proporciona operaciones CRUD sobre la tabla "orden_transferencia"
 * y consultas específicas para el flujo de transferencias.
 */
@Repository
public interface OrdenTransferenciaRepository extends JpaRepository<OrdenTransferencia, Long> {

    // Buscar la orden de transferencia de un trámite específico
    Optional<OrdenTransferencia> findByTramiteTramiteId(Long tramiteId);

    // Buscar órdenes por estado (Generada, Aprobada, Rechazada)
    List<OrdenTransferencia> findByEstado(String estado);

    // Buscar órdenes generadas por una Colectora específica
    List<OrdenTransferencia> findByColectoraUsuarioId(Long colectoraId);

    // Buscar órdenes aprobadas por un Director específico
    List<OrdenTransferencia> findByDirectorUsuarioId(Long directorId);
}
