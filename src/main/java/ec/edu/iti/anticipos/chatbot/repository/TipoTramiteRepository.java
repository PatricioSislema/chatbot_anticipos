package ec.edu.iti.anticipos.chatbot.repository;

import ec.edu.iti.anticipos.chatbot.entity.TipoTramite;
import ec.edu.iti.anticipos.chatbot.entity.Tramite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para la entidad TipoTramite.
 * Proporciona operaciones CRUD sobre la tabla "tipo_tramite".
 */
@Repository
public interface TipoTramiteRepository extends JpaRepository<TipoTramite, Long> {
}