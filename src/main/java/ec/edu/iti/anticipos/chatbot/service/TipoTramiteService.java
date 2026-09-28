package ec.edu.iti.anticipos.chatbot.service;

import ec.edu.iti.anticipos.chatbot.dto.request.TipoTramiteRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.TipoTramiteResponseDTO;
import ec.edu.iti.anticipos.chatbot.entity.TipoTramite;
import ec.edu.iti.anticipos.chatbot.repository.TipoTramiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Servicio para la entidad TipoTramite.
 * Contiene la lógica de negocio relacionada con los tipos de trámite.
 */
@Service
@RequiredArgsConstructor
public class TipoTramiteService {

    private final TipoTramiteRepository tipoTramiteRepository;

    // Listar todos los tipos de trámite
    public List<TipoTramiteResponseDTO> listarTodos() {
        return tipoTramiteRepository.findAll()
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    // Buscar un tipo de trámite por ID
    public Optional<TipoTramiteResponseDTO> buscarPorId(Long id) {
        return tipoTramiteRepository.findById(id)
                .map(this::convertirAResponse);
    }

    // Guardar un nuevo tipo de trámite
    public TipoTramiteResponseDTO guardar(TipoTramiteRequestDTO dto) {
        TipoTramite tipo = new TipoTramite();
        tipo.setNombre(dto.getNombre());

        TipoTramite guardado = tipoTramiteRepository.save(tipo);
        return convertirAResponse(guardado);
    }

    // Eliminar un tipo de trámite por ID
    public void eliminar(Long id) {
        tipoTramiteRepository.deleteById(id);
    }

    // Convertir entidad a DTO de respuesta
    private TipoTramiteResponseDTO convertirAResponse(TipoTramite tipo) {
        TipoTramiteResponseDTO dto = new TipoTramiteResponseDTO();
        dto.setTipoTramiteId(tipo.getTipoTramiteId());
        dto.setNombre(tipo.getNombre());
        return dto;
    }
}