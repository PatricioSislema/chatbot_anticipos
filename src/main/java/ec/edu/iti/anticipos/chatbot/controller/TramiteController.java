package ec.edu.iti.anticipos.chatbot.controller;

import ec.edu.iti.anticipos.chatbot.dto.request.TramiteRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.TramiteResponseDTO;
import ec.edu.iti.anticipos.chatbot.service.TramiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la entidad Tramite.
 * Expone los endpoints para gestionar los trámites.
 */
@RestController
@RequestMapping("/api/tramites")
@RequiredArgsConstructor
public class TramiteController {

    private final TramiteService tramiteService;

    // Listar todos los trámites
    @GetMapping
    public ResponseEntity<List<TramiteResponseDTO>> listar() {
        return ResponseEntity.ok(tramiteService.listarTodos());
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
}