package ec.edu.iti.anticipos.chatbot.service;

import ec.edu.iti.anticipos.chatbot.dto.request.LoginRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.request.UsuarioRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.LoginResponseDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.UsuarioResponseDTO;
import ec.edu.iti.anticipos.chatbot.entity.Rol;
import ec.edu.iti.anticipos.chatbot.entity.Usuario;
import ec.edu.iti.anticipos.chatbot.repository.RolRepository;
import ec.edu.iti.anticipos.chatbot.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Servicio para la entidad Usuario.
 * Contiene la lógica de negocio relacionada con los usuarios y el inicio de sesión.
 * Las contraseñas se almacenan hasheadas con BCrypt.
 * Incluye bloqueo por intentos fallidos.
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    // Listar todos los usuarios
    public List<UsuarioResponseDTO> listarTodos() {
        return usuarioRepository.findAll()
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    // Buscar usuario por ID
    public Optional<UsuarioResponseDTO> buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .map(this::convertirAResponse);
    }

    // Guardar un nuevo usuario
    public UsuarioResponseDTO guardar(UsuarioRequestDTO dto) {
        Usuario usuario = new Usuario();
        usuario.setCedula(dto.getCedula());
        usuario.setNombre(dto.getNombre());
        usuario.setCorreo(dto.getCorreo());
        usuario.setContrasena(passwordEncoder.encode(dto.getContrasena()));
        usuario.setBloqueado(false);
        usuario.setIntentosFallidos(0);

        Rol rol = rolRepository.findById(dto.getRolId())
                .orElseThrow(() -> new RuntimeException("Rol no encontrado"));
        usuario.setRol(rol);

        Usuario guardado = usuarioRepository.save(usuario);
        return convertirAResponse(guardado);
    }

    // Actualizar un usuario existente (sin tocar la contraseña)
    public UsuarioResponseDTO actualizar(Long id, UsuarioRequestDTO dto) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuario.setCedula(dto.getCedula());
        usuario.setNombre(dto.getNombre());
        usuario.setCorreo(dto.getCorreo());

        if (dto.getRolId() != null) {
            Rol rol = rolRepository.findById(dto.getRolId())
                    .orElseThrow(() -> new RuntimeException("Rol no encontrado"));
            usuario.setRol(rol);
        }

        Usuario actualizado = usuarioRepository.save(usuario);
        return convertirAResponse(actualizado);
    }

    // Resetear la contraseña de un usuario
    public void resetearContrasena(Long id, String nuevaContrasena) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        usuario.setContrasena(passwordEncoder.encode(nuevaContrasena));
        usuarioRepository.save(usuario);
    }

    // Desbloquear usuario
    public void desbloquear(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        usuario.setBloqueado(false);
        usuario.setIntentosFallidos(0);
        usuarioRepository.save(usuario);
    }

    // Eliminar usuario por ID
    public void eliminar(Long id) {
        usuarioRepository.deleteById(id);
    }

    // Iniciar sesión con correo y contraseña (usando BCrypt + bloqueo)
    public LoginResponseDTO login(LoginRequestDTO dto) {
        String correo = dto.getCorreo().trim().toLowerCase();
        String contrasena = dto.getContrasena().trim();

        System.out.println("=== LOGIN DEBUG ===");
        System.out.println("Correo recibido: [" + correo + "]");

        List<Usuario> usuarios = usuarioRepository.findAll();

        Optional<Usuario> usuarioOpt = usuarios.stream()
                .filter(u -> u.getCorreo().trim().toLowerCase().equals(correo))
                .findFirst();

        LoginResponseDTO response = new LoginResponseDTO();

        // Usuario no encontrado
        if (usuarioOpt.isEmpty()) {
            response.setAutenticado(false);
            System.out.println("Login fallido: usuario no encontrado");
            return response;
        }

        Usuario usuario = usuarioOpt.get();

        // Usuario bloqueado
        if (Boolean.TRUE.equals(usuario.getBloqueado())) {
            response.setAutenticado(false);
            response.setBloqueado(true);
            System.out.println("Login fallido: usuario bloqueado");
            return response;
        }

        // Contraseña correcta
        if (passwordEncoder.matches(contrasena, usuario.getContrasena())) {
            usuario.setIntentosFallidos(0);
            usuarioRepository.save(usuario);

            response.setUsuarioId(usuario.getUsuarioId());
            response.setNombre(usuario.getNombre());
            response.setCorreo(usuario.getCorreo());
            response.setRolNombre(usuario.getRol().getNombre());
            response.setAutenticado(true);
            response.setBloqueado(false);

            System.out.println("Login exitoso para: " + usuario.getNombre());
            return response;
        }

        // Contraseña incorrecta
        int nuevosIntentos = (usuario.getIntentosFallidos() == null ? 0 : usuario.getIntentosFallidos()) + 1;
        usuario.setIntentosFallidos(nuevosIntentos);

        if (nuevosIntentos >= 3) {
            usuario.setBloqueado(true);
            System.out.println("Usuario BLOQUEADO por 3 intentos fallidos: " + usuario.getCorreo());
        }

        usuarioRepository.save(usuario);

        response.setAutenticado(false);
        response.setIntentosRestantes(3 - nuevosIntentos);
        response.setBloqueado(usuario.getBloqueado());

        System.out.println("Login fallido: contraseña incorrecta. Intentos: " + nuevosIntentos);
        return response;
    }

    // Verificar si un correo existe en la base de datos
    public boolean existeCorreo(String correo) {
        return usuarioRepository.findAll()
                .stream()
                .anyMatch(u -> u.getCorreo().equalsIgnoreCase(correo));
    }

    // Convertir entidad a DTO de respuesta
    private UsuarioResponseDTO convertirAResponse(Usuario usuario) {
        UsuarioResponseDTO dto = new UsuarioResponseDTO();
        dto.setUsuarioId(usuario.getUsuarioId());
        dto.setCedula(usuario.getCedula());
        dto.setNombre(usuario.getNombre());
        dto.setCorreo(usuario.getCorreo());
        dto.setRolNombre(usuario.getRol().getNombre());
        return dto;
    }
}