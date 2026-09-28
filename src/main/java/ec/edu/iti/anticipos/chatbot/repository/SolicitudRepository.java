package ec.edu.iti.anticipos.chatbot.repository;

import ec.edu.iti.anticipos.chatbot.entity.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para la entidad Solicitud.
 * Proporciona operaciones CRUD sobre la tabla "solicitud".
 */
@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {

    // Buscar solicitudes por ID del usuario del trámite
    List<Solicitud> findByTramiteUsuarioUsuarioId(Long usuarioId);

    // Buscar solicitudes por usuario y estado del trámite
    List<Solicitud> findByTramiteUsuarioUsuarioIdAndTramiteEstado(Long usuarioId, String estado);
}