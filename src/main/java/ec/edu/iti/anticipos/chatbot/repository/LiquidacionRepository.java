package ec.edu.iti.anticipos.chatbot.repository;

import ec.edu.iti.anticipos.chatbot.entity.Liquidacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para la entidad Liquidacion.
 * Proporciona operaciones CRUD sobre la tabla "liquidacion".
 */
@Repository
public interface LiquidacionRepository extends JpaRepository<Liquidacion, Long> {

    // Buscar liquidación por ID del trámite
    Optional<Liquidacion> findByTramiteTramiteId(Long tramiteId);
}
