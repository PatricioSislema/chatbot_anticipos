package ec.edu.iti.anticipos.chatbot.repository;

import ec.edu.iti.anticipos.chatbot.entity.Devolucion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para la entidad Devolucion.
 * Proporciona operaciones CRUD sobre la tabla "devolucion".
 */
@Repository
public interface DevolucionRepository extends JpaRepository<Devolucion, Long> {

    // Buscar devoluciones por liquidación
    List<Devolucion> findByLiquidacionLiquidacionId(Long liquidacionId);
}