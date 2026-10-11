package ec.edu.iti.anticipos.chatbot.repository;

import ec.edu.iti.anticipos.chatbot.entity.Reembolso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad Reembolso.
 * Proporciona operaciones CRUD sobre la tabla "reembolso"
 * y consultas específicas para el flujo de reembolsos (caso FALTANTE).
 */
@Repository
public interface ReembolsoRepository extends JpaRepository<Reembolso, Long> {

    // Buscar el reembolso de una liquidación específica
    Optional<Reembolso> findByLiquidacionLiquidacionId(Long liquidacionId);

    // Buscar reembolsos por estado (Pendiente, Autorizado, Rechazado)
    List<Reembolso> findByEstado(String estado);

    // Buscar reembolsos autorizados por un Director específico
    List<Reembolso> findByAutorizadoPorUsuarioId(Long directorId);
}