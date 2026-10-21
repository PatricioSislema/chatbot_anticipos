package ec.edu.iti.anticipos.chatbot.controller;

import ec.edu.iti.anticipos.chatbot.dto.request.LoginRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.request.UsuarioRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.LoginResponseDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.UsuarioResponseDTO;
import ec.edu.iti.anticipos.chatbot.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> listar() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> buscar(@PathVariable Long id) {
        return usuarioService.buscarPorId(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> crear(@RequestBody UsuarioRequestDTO dto) {
        return ResponseEntity.ok(usuarioService.guardar(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> actualizar(@PathVariable Long id, @RequestBody UsuarioRequestDTO dto) {
        return ResponseEntity.ok(usuarioService.actualizar(id, dto));
    }

    @PatchMapping("/{id}/reset-password")
    public ResponseEntity<?> resetearContrasena(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String nueva = body.get("nuevaContrasena");
        if (nueva == null || nueva.isEmpty()) {
            return ResponseEntity.badRequest().body("Falta el campo 'nuevaContrasena'");
        }
        usuarioService.resetearContrasena(id, nueva);
        return ResponseEntity.ok("Contraseña actualizada");
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO dto) {
        return ResponseEntity.ok(usuarioService.login(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        usuarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/desbloquear")
    public ResponseEntity<?> desbloquear(@PathVariable Long id) {
        usuarioService.desbloquear(id);
        return ResponseEntity.ok("Usuario desbloqueado");
    }

    @PatchMapping("/{id}/bloqueo")
    public ResponseEntity<UsuarioResponseDTO> cambiarBloqueo(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        Boolean bloqueado = body.get("bloqueado");
        if (bloqueado == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(usuarioService.cambiarBloqueo(id, bloqueado));
    }
}