package ec.edu.iti.anticipos.chatbot.controller;

import ec.edu.iti.anticipos.chatbot.dto.request.HistorialEstadoRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.HistorialEstadoResponseDTO;
import ec.edu.iti.anticipos.chatbot.service.HistorialEstadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la entidad HistorialEstado.
 * Expone los endpoints para gestionar el historial de cambios de estado.
 */
@RestController
@RequestMapping("/api/historial")
@RequiredArgsConstructor
public class HistorialEstadoController {

    private final HistorialEstadoService historialEstadoService;

    // Listar todos los registros de historial
    @GetMapping
    public ResponseEntity<List<HistorialEstadoResponseDTO>> listar() {
        return ResponseEntity.ok(historialEstadoService.listarTodos());
    }

    // Buscar un registro por ID
    @GetMapping("/{id}")
    public ResponseEntity<HistorialEstadoResponseDTO> buscar(@PathVariable Long id) {
        return historialEstadoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Crear un nuevo registro de cambio de estado
    @PostMapping
    public ResponseEntity<HistorialEstadoResponseDTO> crear(@RequestBody HistorialEstadoRequestDTO dto) {
        return ResponseEntity.ok(historialEstadoService.guardar(dto));
    }

    // Eliminar un registro por ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        historialEstadoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}