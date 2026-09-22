package ec.edu.iti.anticipos.chatbot.repository;

import ec.edu.iti.anticipos.chatbot.entity.TipoTramite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para la entidad TipoTramite.
 * Proporciona operaciones CRUD sobre la tabla "tipo_tramite".
 */
@Repository
public interface TipoTramiteRepository extends JpaRepository<TipoTramite, Long> {
}