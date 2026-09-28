package ec.edu.iti.anticipos.chatbot.controller;

import ec.edu.iti.anticipos.chatbot.dto.request.SolicitudRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.SolicitudResponseDTO;
import ec.edu.iti.anticipos.chatbot.service.SolicitudService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la entidad Solicitud.
 * Expone los endpoints para gestionar las solicitudes de anticipo.
 */
@RestController
@RequestMapping("/api/solicitudes")
@RequiredArgsConstructor
public class SolicitudController {

    private final SolicitudService solicitudService;

    // Listar todas las solicitudes
    @GetMapping
    public ResponseEntity<List<SolicitudResponseDTO>> listar() {
        return ResponseEntity.ok(solicitudService.listarTodos());
    }

    // Buscar una solicitud por ID
    @GetMapping("/{id}")
    public ResponseEntity<SolicitudResponseDTO> buscar(@PathVariable Long id) {
        return solicitudService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Crear una nueva solicitud
    @PostMapping
    public ResponseEntity<SolicitudResponseDTO> crear(@RequestBody SolicitudRequestDTO dto) {
        return ResponseEntity.ok(solicitudService.guardar(dto));
    }

    // Eliminar una solicitud por ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        solicitudService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
