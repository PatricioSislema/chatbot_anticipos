package ec.edu.iti.anticipos.chatbot.repository;

import ec.edu.iti.anticipos.chatbot.entity.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para la entidad Factura.
 * Proporciona operaciones CRUD sobre la tabla "factura".
 */
@Repository
public interface FacturaRepository extends JpaRepository<Factura, Long> {
}
