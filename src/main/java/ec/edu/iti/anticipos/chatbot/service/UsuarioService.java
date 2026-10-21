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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Servicio para la entidad Usuario.
 * Contiene la lógica de negocio relacionada con los usuarios y el inicio de sesión.
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    // Listar todos los usuarios
    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listarTodos() {
        return usuarioRepository.findAll()
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    public Optional<UsuarioResponseDTO> buscarPorId(Long id) {
        return usuarioRepository.findById(id).map(this::convertirAResponse);
    }

    public UsuarioResponseDTO guardar(UsuarioRequestDTO dto) {
        Usuario usuario = new Usuario();
        usuario.setCedula(dto.getCedula().trim());
        usuario.setNombre(dto.getNombre().trim().toUpperCase());
        usuario.setCorreo(dto.getCorreo().trim().toLowerCase());
        usuario.setTelefono(dto.getTelefono());
        usuario.setCargo(dto.getCargo() != null ? dto.getCargo().toUpperCase() : null);
        usuario.setContrasena(passwordEncoder.encode(dto.getContrasena()));
        usuario.setBloqueado(false);
        usuario.setIntentosFallidos(0);
        usuario.setJefeInmediato(dto.getJefeInmediatoId() != null ? usuarioRepository.findById(dto.getJefeInmediatoId())
                .orElse(null) : null);

        Rol rol = rolRepository.findById(dto.getRolId())
                .orElseThrow(() -> new RuntimeException("Rol no encontrado"));
        usuario.setRol(rol);

        Usuario guardado = usuarioRepository.save(usuario);
        return convertirAResponse(guardado);
    }

    public UsuarioResponseDTO actualizar(Long id, UsuarioRequestDTO dto) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Limpiamos el correo si viene con prefijo
        String correoLimpio = dto.getCorreo().trim().toLowerCase();
        if (correoLimpio.startsWith("eliminado_")) {
            correoLimpio = correoLimpio.replaceFirst("^eliminado_\\d+_", "");
        } else if (correoLimpio.matches("^\\d+_.*")) {
            correoLimpio = correoLimpio.replaceFirst("^\\d+_", "");
        }

        usuario.setCedula(dto.getCedula().trim());
        usuario.setNombre(dto.getNombre().trim().toUpperCase());
        usuario.setCorreo(correoLimpio);
        usuario.setTelefono(dto.getTelefono());
        usuario.setCargo(dto.getCargo());
        usuario.setJefeInmediato(dto.getJefeInmediatoId() != null ? usuarioRepository.findById(dto.getJefeInmediatoId())
                .orElse(null) : null);

        if (dto.getRolId() != null) {
            Rol rol = rolRepository.findById(dto.getRolId())
                    .orElseThrow(() -> new RuntimeException("Rol no encontrado"));
            usuario.setRol(rol);
        }

        Usuario actualizado = usuarioRepository.save(usuario);
        return convertirAResponse(actualizado);
    }

    public void resetearContrasena(Long id, String nuevaContrasena) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        usuario.setContrasena(passwordEncoder.encode(nuevaContrasena));
        usuarioRepository.save(usuario);
    }

    public void desbloquear(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        usuario.setBloqueado(false);
        usuario.setIntentosFallidos(0);

        // Restaurar correo
        String correoActual = usuario.getCorreo();
        if (correoActual != null) {
            if (correoActual.startsWith("eliminado_")) {
                correoActual = correoActual.replaceFirst("^eliminado_\\d+_", "");
            } else if (correoActual.matches("^\\d+_.*")) {
                correoActual = correoActual.replaceFirst("^\\d+_", "");
            }
            usuario.setCorreo(correoActual);
        }

        usuarioRepository.save(usuario);
    }

    public UsuarioResponseDTO cambiarBloqueo(Long id, boolean bloqueado) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuario.setBloqueado(bloqueado);
        if (!bloqueado) {
            usuario.setIntentosFallidos(0);
        }

        // Restaurar correo SIEMPRE (por si quedó con prefijo de pruebas anteriores)
        String correoActual = usuario.getCorreo();
        if (correoActual != null) {
            if (correoActual.startsWith("eliminado_")) {
                correoActual = correoActual.replaceFirst("^eliminado_\\d+_", "");
            } else if (correoActual.matches("^\\d+_.*")) {
                correoActual = correoActual.replaceFirst("^\\d+_", "");
            }
            usuario.setCorreo(correoActual);
        }

        Usuario actualizado = usuarioRepository.save(usuario);
        return convertirAResponse(actualizado);
    }

    public void eliminar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // SOLO BLOQUEAMOS, NO CAMBIAMOS EL CORREO
        usuario.setBloqueado(true);
        usuario.setTelegramChatId(null);
        usuarioRepository.save(usuario);
    }

    public LoginResponseDTO login(LoginRequestDTO dto) {
        String correo = dto.getCorreo().trim().toLowerCase();
        String contrasena = dto.getContrasena().trim();

        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(correo);
        LoginResponseDTO response = new LoginResponseDTO();

        if (usuarioOpt.isEmpty()) {
            response.setAutenticado(false);
            return response;
        }

        Usuario usuario = usuarioOpt.get();

        if (Boolean.TRUE.equals(usuario.getBloqueado())) {
            response.setAutenticado(false);
            response.setBloqueado(true);
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
            return response;
        }

        int nuevosIntentos = (usuario.getIntentosFallidos() == null ? 0 : usuario.getIntentosFallidos()) + 1;
        usuario.setIntentosFallidos(nuevosIntentos);

        if (nuevosIntentos >= 3) {
            usuario.setBloqueado(true);
        }
        usuarioRepository.save(usuario);

        response.setAutenticado(false);
        response.setIntentosRestantes(3 - nuevosIntentos);
        response.setBloqueado(usuario.getBloqueado());
        return response;
    }

    public boolean existeCorreo(String correo) {
        return usuarioRepository.existsByCorreo(correo);
    }

    public void actualizarTelegramChatId(Long usuarioId, String chatId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        usuario.setTelegramChatId(chatId);
        usuarioRepository.save(usuario);
    }

    public String obtenerTelegramChatIdPorCorreo(String correo) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(correo);
        return usuarioOpt.map(Usuario::getTelegramChatId).orElse(null);
    }

    public String obtenerTelegramChatIdPorUsuarioId(Long usuarioId) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(usuarioId);
        return usuarioOpt.map(Usuario::getTelegramChatId).orElse(null);
    }

    public String obtenerCorreoPorRol(String rolNombre) {
        return usuarioRepository.findByRolNombre(rolNombre)
                .stream()
                .findFirst()
                .map(Usuario::getCorreo)
                .orElse(null);
    }

    public String obtenerCorreoJefeInmediato(Long usuarioId) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(usuarioId);
        if (usuarioOpt.isEmpty()) return null;
        Usuario jefe = usuarioOpt.get().getJefeInmediato();
        return jefe != null ? jefe.getCorreo() : null;
    }

    // ==========================================
    // CONVERSIÓN A DTO
    // ==========================================
    private UsuarioResponseDTO convertirAResponse(Usuario usuario) {
        UsuarioResponseDTO dto = new UsuarioResponseDTO();
        dto.setUsuarioId(usuario.getUsuarioId());
        dto.setCedula(usuario.getCedula());
        dto.setNombre(usuario.getNombre());
        dto.setTelefono(usuario.getTelefono());
        dto.setCargo(usuario.getCargo());
        dto.setCorreo(usuario.getCorreo());
        dto.setRolNombre(usuario.getRol().getNombre());

        dto.setBloqueado(usuario.getBloqueado());
        dto.setIntentosFallidos(usuario.getIntentosFallidos());
        dto.setTelegramChatId(usuario.getTelegramChatId());

        if (usuario.getJefeInmediato() != null) {
            dto.setJefeInmediatoId(usuario.getJefeInmediato().getUsuarioId());
            dto.setJefeNombre(usuario.getJefeInmediato().getNombre());
        }
        return dto;
    }
}