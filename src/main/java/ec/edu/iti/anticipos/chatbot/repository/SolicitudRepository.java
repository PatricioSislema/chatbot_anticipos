package ec.edu.iti.anticipos.chatbot.repository;

import ec.edu.iti.anticipos.chatbot.entity.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para la entidad Solicitud.
 * Proporciona operaciones CRUD sobre la tabla "solicitud".
 */
@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {
}