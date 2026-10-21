package ec.edu.iti.anticipos.chatbot.repository;

import ec.edu.iti.anticipos.chatbot.entity.Tramite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para la entidad Tramite.
 * Proporciona operaciones CRUD y consultas filtradas con paginación.
 */
@Repository
public interface TramiteRepository extends JpaRepository<Tramite, Long> {

    // ¡ESTA ES LA SOLUCIÓN! Sobrescribimos el findAll para traer al usuario y tipo de trámite de una vez (JOIN FETCH)
    @Override
    @Query("SELECT t FROM Tramite t JOIN FETCH t.usuario JOIN FETCH t.tipoTramite")
    List<Tramite> findAll();

    // Buscar trámites por estado ("Pendiente")
    List<Tramite> findByEstado(String estado);

    // Buscar trámites por ID de usuario
    List<Tramite> findByUsuarioUsuarioId(Long usuarioId);

    // Contar trámites por estado exacto (para el dashboard)
    long countByEstado(String estado);

    // Consulta filtrada con paginación (para la tabla del dashboard)
    @Query("SELECT t FROM Tramite t WHERE " +
            "(:estado IS NULL OR t.estado = :estado) AND " +
            "(:usuarioId IS NULL OR t.usuario.usuarioId = :usuarioId) " +
            "ORDER BY t.fechaCreacion DESC, t.tramiteId DESC")
    Page<Tramite> filtrar(@Param("estado") String estado,
                          @Param("usuarioId") Long usuarioId,
                          Pageable pageable);

    // Buscar trámites por usuario y varios estados
    @Query("SELECT t FROM Tramite t WHERE t.usuario.usuarioId = :usuarioId " +
            "AND t.estado IN :estados " +
            "ORDER BY t.fechaCreacion DESC")
    List<Tramite> buscarPorUsuarioYEstados(@Param("usuarioId") Long usuarioId,
                                           @Param("estados") List<String> estados);

    // Buscar cuántos trámites usan un tipo específico (para validar eliminación)
    long countByTipoTramiteTipoTramiteId(Long tipoTramiteId);

}