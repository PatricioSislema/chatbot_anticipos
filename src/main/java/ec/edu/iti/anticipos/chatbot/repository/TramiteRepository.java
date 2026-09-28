package ec.edu.iti.anticipos.chatbot.repository;

import ec.edu.iti.anticipos.chatbot.entity.Tramite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para la entidad Tramite.
 * Proporciona operaciones CRUD sobre la tabla "tramite".
 */
@Repository
public interface TramiteRepository extends JpaRepository<Tramite, Long> {

    // Buscar trámites por estado ("Pendiente")
    List<Tramite> findByEstado(String estado);

    // Buscar trámites por ID de usuario
    List<Tramite> findByUsuarioUsuarioId(Long usuarioId);
}