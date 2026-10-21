package ec.edu.iti.anticipos.chatbot.service;

import ec.edu.iti.anticipos.chatbot.dto.request.RolRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.RolResponseDTO;
import ec.edu.iti.anticipos.chatbot.entity.Rol;
import ec.edu.iti.anticipos.chatbot.repository.RolRepository;
import ec.edu.iti.anticipos.chatbot.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

/**
 * Servicio para la entidad Rol.
 * Contiene la lógica de negocio relacionada con los roles del sistema.
 */
@Service
@RequiredArgsConstructor
public class RolService {

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;

    public List<RolResponseDTO> listarTodos() {
        return rolRepository.findAll()
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    public Optional<RolResponseDTO> buscarPorId(Long id) {
        return rolRepository.findById(id)
                .map(this::convertirAResponse);
    }

    public RolResponseDTO guardar(RolRequestDTO dto) {
        Rol rol = new Rol();
        rol.setNombre(dto.getNombre().toUpperCase().trim());
        rol.setDescripcion(dto.getDescripcion());
        rol.setActivo(true);

        Rol guardado = rolRepository.save(rol);
        return convertirAResponse(guardado);
    }

    public RolResponseDTO actualizar(Long id, RolRequestDTO dto) {
        Rol rol = rolRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado"));

        rol.setNombre(dto.getNombre().toUpperCase().trim());
        rol.setDescripcion(dto.getDescripcion());
        if (dto.getActivo() != null) {
            rol.setActivo(dto.getActivo());
        }

        Rol actualizado = rolRepository.save(rol);
        return convertirAResponse(actualizado);
    }

    // BORRADO LÓGICO CON VALIDACIÓN Y MANEJO DE ERRORES HTTP CORRECTO
    public void eliminar(Long id) {
        Rol rol = rolRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado"));

        long usuariosConRol = usuarioRepository.findByRolNombreIgnoreCase(rol.getNombre()).size();
        if (usuariosConRol > 0) {
            throw new RuntimeException("No se puede desactivar: " + usuariosConRol + " usuario(s) tienen asignado este rol.");
        }

        rol.setActivo(false);
        rolRepository.save(rol);
    }

    private RolResponseDTO convertirAResponse(Rol rol) {
        RolResponseDTO dto = new RolResponseDTO();
        dto.setId(rol.getRolId());
        dto.setNombre(rol.getNombre());
        dto.setDescripcion(rol.getDescripcion());
        dto.setActivo(rol.getActivo());
        return dto;
    }
}