package ec.edu.iti.anticipos.chatbot.repository;

import ec.edu.iti.anticipos.chatbot.entity.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para la entidad Notificacion.
 * Proporciona operaciones CRUD sobre la tabla "notificacion"
 * y consultas específicas por trámite y destinatario.
 */
@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    // Buscar todas las notificaciones de un trámite
    List<Notificacion> findByTramiteTramiteId(Long tramiteId);

    // Buscar todas las notificaciones enviadas a un destinatario (correo)
    List<Notificacion> findByDestinatario(String destinatario);

    // Buscar notificaciones por canal (Correo, Telegram, Ambos)
    List<Notificacion> findByCanal(String canal);

    // Buscar notificaciones de un trámite en un canal específico
    List<Notificacion> findByTramiteTramiteIdAndCanal(Long tramiteId, String canal);
}