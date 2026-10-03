package ec.edu.iti.anticipos.chatbot.controller;

import ec.edu.iti.anticipos.chatbot.dto.request.TramiteRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.TramiteResponseDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.TramiteResumenDTO;
import ec.edu.iti.anticipos.chatbot.entity.Tramite;
import ec.edu.iti.anticipos.chatbot.repository.TramiteRepository;
import ec.edu.iti.anticipos.chatbot.service.TramiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la entidad Tramite.
 * Soporta filtros por estado y usuario, y paginación para el dashboard.
 */
@RestController
@RequestMapping("/api/tramites")
@RequiredArgsConstructor
public class TramiteController {

    private final TramiteService tramiteService;
    private final TramiteRepository tramiteRepository;

    // Listar TODOS (sin paginar, se mantiene para compatibilidad con el bot)
    @GetMapping("/todos")
    public ResponseEntity<List<TramiteResponseDTO>> listarTodos() {
        return ResponseEntity.ok(tramiteService.listarTodos());
    }

    // Listar con filtros + paginación (para el dashboard)
    @GetMapping
    public ResponseEntity<Page<TramiteResumenDTO>> listar(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Tramite> resultado = tramiteRepository.filtrar(estado, usuarioId, pageable);

        Page<TramiteResumenDTO> dto = resultado.map(this::convertirAResumen);
        return ResponseEntity.ok(dto);
    }

    // Buscar un trámite por ID
    @GetMapping("/{id}")
    public ResponseEntity<TramiteResponseDTO> buscar(@PathVariable Long id) {
        return tramiteService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Crear un nuevo trámite
    @PostMapping
    public ResponseEntity<TramiteResponseDTO> crear(@RequestBody TramiteRequestDTO dto) {
        return ResponseEntity.ok(tramiteService.guardar(dto));
    }

    // Actualizar el estado de un trámite
    @PutMapping("/{id}/estado")
    public ResponseEntity<TramiteResponseDTO> actualizarEstado(
            @PathVariable Long id,
            @RequestParam String estado) {
        return ResponseEntity.ok(tramiteService.actualizarEstado(id, estado));
    }

    // Eliminar un trámite por ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        tramiteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // Convertir entidad a DTO resumido
    private TramiteResumenDTO convertirAResumen(Tramite t) {
        TramiteResumenDTO dto = new TramiteResumenDTO();
        dto.setTramiteId(t.getTramiteId());
        dto.setEstado(t.getEstado());
        dto.setFechaCreacion(t.getFechaCreacion());
        dto.setNombreUsuario(t.getUsuario().getNombre());
        dto.setNombreTipoTramite(t.getTipoTramite().getNombre());
        return dto;
    }
}