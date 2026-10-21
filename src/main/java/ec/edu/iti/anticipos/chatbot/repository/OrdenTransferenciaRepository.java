package ec.edu.iti.anticipos.chatbot.repository;

import ec.edu.iti.anticipos.chatbot.entity.OrdenTransferencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para la entidad OrdenTransferencia.
 * Proporciona operaciones CRUD sobre la tabla "orden_transferencia"
 * y consultas específicas para el flujo de transferencias.
 */
@Repository
public interface OrdenTransferenciaRepository extends JpaRepository<OrdenTransferencia, Long> {

    // Buscar la orden de transferencia de un trámite específico (Cambiado a List)
    List<OrdenTransferencia> findByTramiteTramiteId(Long tramiteId);

    // Buscar órdenes por estado (Generada, Aprobada, Rechazada)
    List<OrdenTransferencia> findByEstado(String estado);

    // Buscar órdenes generadas por una Colectora específica
    List<OrdenTransferencia> findByColectoraUsuarioId(Long colectoraId);

    // Buscar órdenes aprobadas por un Director específico
    List<OrdenTransferencia> findByDirectorUsuarioId(Long directorId);

    @Override
    @Query("SELECT o FROM OrdenTransferencia o " +
            "JOIN FETCH o.tramite t " +
            "JOIN FETCH t.usuario u " +
            "LEFT JOIN FETCH o.colectora " +
            "LEFT JOIN FETCH o.director")
    List<OrdenTransferencia> findAll();
}