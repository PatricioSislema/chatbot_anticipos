package ec.edu.iti.anticipos.chatbot.controller;

import ec.edu.iti.anticipos.chatbot.dto.request.RolRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.RolResponseDTO;
import ec.edu.iti.anticipos.chatbot.service.RolService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RolController {

    private final RolService rolService;

    @GetMapping
    public ResponseEntity<List<RolResponseDTO>> listar() {
        return ResponseEntity.ok(rolService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RolResponseDTO> buscar(@PathVariable Long id) {
        return rolService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<RolResponseDTO> crear(@RequestBody RolRequestDTO dto) {
        return ResponseEntity.ok(rolService.guardar(dto));
    }

    // Endpoint para actualizar (Lo usa el botón editar del frontend)
    @PutMapping("/{id}")
    public ResponseEntity<RolResponseDTO> actualizar(@PathVariable Long id, @RequestBody RolRequestDTO dto) {
        return ResponseEntity.ok(rolService.actualizar(id, dto));
    }

    //  Endpoint  borrado lógico
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            rolService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            // Devolvemos un 400 (Bad Request) con un JSON formateado
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        }
    }
}