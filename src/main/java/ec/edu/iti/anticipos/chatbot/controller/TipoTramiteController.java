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

    // Eliminar un tipo de trámite por ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        tipoTramiteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}