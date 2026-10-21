package ec.edu.iti.anticipos.chatbot.controller;

import ec.edu.iti.anticipos.chatbot.dto.request.TipoTramiteRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.TipoTramiteResponseDTO;
import ec.edu.iti.anticipos.chatbot.service.TipoTramiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la entidad TipoTramite.
 * Expone los endpoints para gestionar los tipos de trámite.
 */
@RestController
@RequestMapping("/api/tipos-tramite")
@RequiredArgsConstructor
public class TipoTramiteController {

    private final TipoTramiteService tipoTramiteService;

    // Listar todos los tipos de trámite
    @GetMapping
    public ResponseEntity<List<TipoTramiteResponseDTO>> listar() {
        return ResponseEntity.ok(tipoTramiteService.listarTodos());
    }

    // Buscar un tipo de trámite por ID
    @GetMapping("/{id}")
    public ResponseEntity<TipoTramiteResponseDTO> buscar(@PathVariable Long id) {
        return tipoTramiteService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Crear un nuevo tipo de trámite
    @PostMapping
    public ResponseEntity<TipoTramiteResponseDTO> crear(@RequestBody TipoTramiteRequestDTO dto) {
        return ResponseEntity.ok(tipoTramiteService.guardar(dto));
    }

    // Actualizar un tipo de trámite
    @PutMapping("/{id}")
    public ResponseEntity<TipoTramiteResponseDTO> actualizar(
            @PathVariable Long id,
            @RequestBody TipoTramiteRequestDTO dto) {
        return ResponseEntity.ok(tipoTramiteService.actualizar(id, dto));
    }

    // Eliminar un tipo de trámite por ID
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            tipoTramiteService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        }
    }
}