package ec.edu.iti.anticipos.chatbot.repository;

import ec.edu.iti.anticipos.chatbot.entity.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para la entidad Notificacion.
 * Proporciona operaciones CRUD sobre la tabla "notificacion".
 */
@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
}