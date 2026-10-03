package ec.edu.iti.anticipos.chatbot.service;

import ec.edu.iti.anticipos.chatbot.dto.request.LoginRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.LoginResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para el servicio de Usuario.
 * Verifica el login con credenciales correctas e incorrectas.
 */
@SpringBootTest
class UsuarioServiceTest {

    @Autowired
    private UsuarioService usuarioService;

    @Test
    void loginConCredencialesCorrectas() {
        // Preparar
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setCorreo("juan.perez@iti.edu.ec");
        dto.setContrasena("123456");

        // Ejecutar
        LoginResponseDTO response = usuarioService.login(dto);

        // Verificar
        assertTrue(response.isAutenticado());
        assertEquals("Juan Pérez", response.getNombre());
        assertEquals("Docente", response.getRolNombre());
    }

    @Test
    void loginConCredencialesIncorrectas() {
        // Preparar
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setCorreo("juan.perez@iti.edu.ec");
        dto.setContrasena("claveincorrecta");

        // Ejecutar
        LoginResponseDTO response = usuarioService.login(dto);

        // Verificar
        assertFalse(response.isAutenticado());
    }
}