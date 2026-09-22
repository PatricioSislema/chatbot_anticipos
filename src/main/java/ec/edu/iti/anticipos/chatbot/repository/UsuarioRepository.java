package ec.edu.iti.anticipos.chatbot.repository;

import ec.edu.iti.anticipos.chatbot.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para la entidad Usuario.
 * Proporciona operaciones CRUD sobre la tabla "usuario".
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
}