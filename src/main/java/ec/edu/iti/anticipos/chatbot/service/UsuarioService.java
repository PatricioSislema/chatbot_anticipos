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
 * Incluye bloqueo por intentos fallidos y gestión del chatId de Telegram.
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

        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(correo);

        LoginResponseDTO response = new LoginResponseDTO();

        if (usuarioOpt.isEmpty()) {
            response.setAutenticado(false);
            System.out.println("Login fallido: usuario no encontrado");
            return response;
        }

        Usuario usuario = usuarioOpt.get();

        if (Boolean.TRUE.equals(usuario.getBloqueado())) {
            response.setAutenticado(false);
            response.setBloqueado(true);
            System.out.println("Login fallido: usuario bloqueado");
            return response;
        }

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
        return usuarioRepository.existsByCorreo(correo);
    }

    // ============================================================
    // MÉTODOS DE TELEGRAM
    // ============================================================

    /**
     * Actualizar el chatId de Telegram de un usuario.
     */
    public void actualizarTelegramChatId(Long usuarioId, String chatId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        usuario.setTelegramChatId(chatId);
        usuarioRepository.save(usuario);
    }

    /**
     * Obtener el chatId de Telegram de un usuario por su correo.
     * Retorna null si el usuario no tiene chatId asignado o no existe.
     */
    public String obtenerTelegramChatIdPorCorreo(String correo) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(correo);
        return usuarioOpt.map(Usuario::getTelegramChatId).orElse(null);
    }

    /**
     * Obtener el chatId de Telegram de un usuario por su ID.
     * Retorna null si el usuario no tiene chatId asignado o no existe.
     */
    public String obtenerTelegramChatIdPorUsuarioId(Long usuarioId) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(usuarioId);
        return usuarioOpt.map(Usuario::getTelegramChatId).orElse(null);
    }

    // ============================================================
    // MÉTODOS HELPER PARA NOTIFICACIONES DINÁMICAS (NUEVOS)
    // ============================================================

    /**
     * Obtener el correo del primer usuario que tenga un rol específico.
     * Útil para notificar a "la Contadora", "el Director", etc.
     */
    public String obtenerCorreoPorRol(String rolNombre) {
        return usuarioRepository.findByRolNombre(rolNombre)
                .stream()
                .findFirst()
                .map(Usuario::getCorreo)
                .orElse(null);
    }

    /**
     * Obtener el correo del jefe inmediato de un usuario.
     * Retorna null si el usuario no tiene jefe asignado o no existe.
     */
    public String obtenerCorreoJefeInmediato(Long usuarioId) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(usuarioId);
        if (usuarioOpt.isEmpty()) return null;

        Usuario jefe = usuarioOpt.get().getJefeInmediato();
        return jefe != null ? jefe.getCorreo() : null;
    }

    // ============================================================
    // CONVERSIÓN A DTO
    // ============================================================

    private UsuarioResponseDTO convertirAResponse(Usuario usuario) {
        UsuarioResponseDTO dto = new UsuarioResponseDTO();
        dto.setUsuarioId(usuario.getUsuarioId());
        dto.setCedula(usuario.getCedula());
        dto.setNombre(usuario.getNombre());
        dto.setCorreo(usuario.getCorreo());
        dto.setRolNombre(usuario.getRol().getNombre());
        dto.setBloqueado(usuario.getBloqueado());
        dto.setIntentosFallidos(usuario.getIntentosFallidos());
        dto.setTelegramChatId(usuario.getTelegramChatId());
        return dto;
    }
}