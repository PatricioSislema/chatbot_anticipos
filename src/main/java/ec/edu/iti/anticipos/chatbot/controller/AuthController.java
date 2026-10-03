package ec.edu.iti.anticipos.chatbot.controller;

import ec.edu.iti.anticipos.chatbot.dto.request.LoginRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.AuthResponseDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.LoginResponseDTO;
import ec.edu.iti.anticipos.chatbot.security.JwtUtil;
import ec.edu.iti.anticipos.chatbot.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para autenticación desde el dashboard web.
 * Devuelve un token JWT que el frontend usará en cada petición.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioService usuarioService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO dto) {

        // 1. Validar credenciales con la lógica existente
        LoginResponseDTO response = usuarioService.login(dto);

        if (!response.isAutenticado()) {
            return ResponseEntity.status(401).body("Credenciales incorrectas");
        }

        // 2. Generar el token JWT
        String token = jwtUtil.generarToken(
                response.getCorreo(),
                response.getRolNombre(),
                response.getUsuarioId()
        );

        // 3. Devolver el token + datos básicos
        AuthResponseDTO auth = new AuthResponseDTO(
                token,
                response.getRolNombre(),
                response.getNombre(),
                response.getCorreo(),
                response.getUsuarioId()
        );

        return ResponseEntity.ok(auth);
    }
}
