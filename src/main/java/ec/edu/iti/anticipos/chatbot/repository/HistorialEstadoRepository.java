package ec.edu.iti.anticipos.chatbot.repository;

import ec.edu.iti.anticipos.chatbot.entity.HistorialEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para la entidad HistorialEstado.
 * Proporciona operaciones CRUD sobre la tabla "historial_estado".
 */
@Repository
public interface HistorialEstadoRepository extends JpaRepository<HistorialEstado, Long> {
}
