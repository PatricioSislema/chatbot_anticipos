package ec.edu.iti.anticipos.chatbot.controller;

import ec.edu.iti.anticipos.chatbot.dto.request.NotificacionRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.NotificacionResponseDTO;
import ec.edu.iti.anticipos.chatbot.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la entidad Notificacion.
 * Expone los endpoints para gestionar las notificaciones del sistema.
 */
@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;

    // Listar todas las notificaciones
    @GetMapping
    public ResponseEntity<List<NotificacionResponseDTO>> listar() {
        return ResponseEntity.ok(notificacionService.listarTodos());
    }

    // Buscar una notificación por ID
    @GetMapping("/{id}")
    public ResponseEntity<NotificacionResponseDTO> buscar(@PathVariable Long id) {
        return notificacionService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Crear una nueva notificación
    @PostMapping
    public ResponseEntity<NotificacionResponseDTO> crear(@RequestBody NotificacionRequestDTO dto) {
        return ResponseEntity.ok(notificacionService.guardar(dto));
    }

    // Eliminar una notificación por ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        notificacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}