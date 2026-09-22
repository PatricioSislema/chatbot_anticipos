package ec.edu.iti.anticipos.chatbot.service;

import ec.edu.iti.anticipos.chatbot.dto.request.RolRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.RolResponseDTO;
import ec.edu.iti.anticipos.chatbot.entity.Rol;
import ec.edu.iti.anticipos.chatbot.repository.RolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Servicio para la entidad Rol.
 * Contiene la lógica de negocio relacionada con los roles del sistema.
 */
@Service
@RequiredArgsConstructor
public class RolService {

    // Inyección del repositorio
    private final RolRepository rolRepository;

    // Listar todos los roles
    public List<RolResponseDTO> listarTodos() {
        return rolRepository.findAll()
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    // Buscar un rol por ID
    public Optional<RolResponseDTO> buscarPorId(Long id) {
        return rolRepository.findById(id)
                .map(this::convertirAResponse);
    }

    // Guardar un nuevo rol
    public RolResponseDTO guardar(RolRequestDTO dto) {
        Rol rol = new Rol();
        rol.setNombre(dto.getNombre());
        rol.setDescripcion(dto.getDescripcion());

        Rol guardado = rolRepository.save(rol);
        return convertirAResponse(guardado);
    }

    // Eliminar un rol por ID
    public void eliminar(Long id) {
        rolRepository.deleteById(id);
    }

    // Convertir entidad a DTO de respuesta
    private RolResponseDTO convertirAResponse(Rol rol) {
        RolResponseDTO dto = new RolResponseDTO();
        dto.setId(rol.getRolId());
        dto.setNombre(rol.getNombre());
        dto.setDescripcion(rol.getDescripcion());
        return dto;
    }
}