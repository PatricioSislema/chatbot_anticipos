package ec.edu.iti.anticipos.chatbot.repository;

import ec.edu.iti.anticipos.chatbot.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad Usuario.
 * Proporciona operaciones CRUD sobre la tabla "usuario"
 * y consultas específicas para el chatbot.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Buscar un usuario por correo (usado en el login)
    Optional<Usuario> findByCorreo(String correo);

    // Verificar si un correo existe (evita traer toda la lista)
    boolean existsByCorreo(String correo);

    // Buscar usuario por el chat_id de Telegram
    Optional<Usuario> findByTelegramChatId(String telegramChatId);

    // Buscar todos los usuarios de un rol específico (para notificaciones)
    List<Usuario> findByRolNombre(String rolNombre);

    // Buscar los subordinados (usuarios cuyo jefe inmediato es este usuario)
    List<Usuario> findByJefeInmediatoUsuarioId(Long jefeInmediatoId);

    // Busca ignorando mayúsculas y minúsculas
    List<Usuario> findByRolNombreIgnoreCase(String rolNombre);
}