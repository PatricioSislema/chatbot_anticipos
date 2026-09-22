package ec.edu.iti.anticipos.chatbot.repository;

import ec.edu.iti.anticipos.chatbot.entity.Tramite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para la entidad Tramite.
 * Proporciona operaciones CRUD sobre la tabla "tramite".
 */
@Repository
public interface TramiteRepository extends JpaRepository<Tramite, Long> {
}