package ec.edu.iti.anticipos.chatbot.controller;

import ec.edu.iti.anticipos.chatbot.dto.request.LoginRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.request.UsuarioRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.LoginResponseDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.UsuarioResponseDTO;
import ec.edu.iti.anticipos.chatbot.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la entidad Usuario.
 * Expone los endpoints para gestionar usuarios y el inicio de sesión.
 * La mayoría de endpoints requieren rol ADMINISTRADOR.
 */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    // Listar todos los usuarios (solo Administrador)
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<UsuarioResponseDTO>> listar() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    // Buscar un usuario por ID (solo Administrador)
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UsuarioResponseDTO> buscar(@PathVariable Long id) {
        return usuarioService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Crear un nuevo usuario (solo Administrador)
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UsuarioResponseDTO> crear(@RequestBody UsuarioRequestDTO dto) {
        return ResponseEntity.ok(usuarioService.guardar(dto));
    }

    // Actualizar un usuario existente (solo Administrador)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UsuarioResponseDTO> actualizar(
            @PathVariable Long id,
            @RequestBody UsuarioRequestDTO dto) {
        return ResponseEntity.ok(usuarioService.actualizar(id, dto));
    }

    // Resetear la contraseña de un usuario (solo Administrador)
    @PatchMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> resetearContrasena(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String nueva = body.get("nuevaContrasena");
        if (nueva == null || nueva.isEmpty()) {
            return ResponseEntity.badRequest().body("Falta el campo 'nuevaContrasena'");
        }
        usuarioService.resetearContrasena(id, nueva);
        return ResponseEntity.ok("Contraseña actualizada");
    }

    // Iniciar sesión (público, para el bot)
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO dto) {
        return ResponseEntity.ok(usuarioService.login(dto));
    }

    // Eliminar un usuario por ID (solo Administrador)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        usuarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}