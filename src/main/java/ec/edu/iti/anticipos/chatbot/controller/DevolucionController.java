package ec.edu.iti.anticipos.chatbot.controller;

import ec.edu.iti.anticipos.chatbot.dto.request.DevolucionRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.DevolucionResponseDTO;
import ec.edu.iti.anticipos.chatbot.service.DevolucionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la entidad Devolucion.
 * Expone los endpoints para gestionar las devoluciones de excedentes.
 */
@RestController
@RequestMapping("/api/devoluciones")
@RequiredArgsConstructor
public class DevolucionController {

    private final DevolucionService devolucionService;

    // Listar todas las devoluciones
    @GetMapping
    public ResponseEntity<List<DevolucionResponseDTO>> listar() {
        return ResponseEntity.ok(devolucionService.listarTodos());
    }

    // Buscar una devolución por ID
    @GetMapping("/{id}")
    public ResponseEntity<DevolucionResponseDTO> buscar(@PathVariable Long id) {
        return devolucionService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Crear una nueva devolución
    @PostMapping
    public ResponseEntity<DevolucionResponseDTO> crear(@RequestBody DevolucionRequestDTO dto) {
        return ResponseEntity.ok(devolucionService.guardar(dto));
    }

    // Eliminar una devolución por ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        devolucionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
