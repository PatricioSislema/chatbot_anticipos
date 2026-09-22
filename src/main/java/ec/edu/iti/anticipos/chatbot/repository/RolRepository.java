package ec.edu.iti.anticipos.chatbot.repository;

import ec.edu.iti.anticipos.chatbot.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/*
 * Repositorio de la entidad Rol
 * Propórciona las operaciones CRUD de la tabla rol
 * */

@Repository

public interface RolRepository extends JpaRepository<Rol,Long> {

}
