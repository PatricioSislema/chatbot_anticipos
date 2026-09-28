package ec.edu.iti.anticipos.chatbot.service;

import ec.edu.iti.anticipos.chatbot.dto.request.HistorialEstadoRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.HistorialEstadoResponseDTO;
import ec.edu.iti.anticipos.chatbot.entity.HistorialEstado;
import ec.edu.iti.anticipos.chatbot.entity.Tramite;
import ec.edu.iti.anticipos.chatbot.entity.Usuario;
import ec.edu.iti.anticipos.chatbot.repository.HistorialEstadoRepository;
import ec.edu.iti.anticipos.chatbot.repository.TramiteRepository;
import ec.edu.iti.anticipos.chatbot.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Servicio para la entidad HistorialEstado.
 * Contiene la lógica de negocio relacionada con el registro de cambios de estado
 * de los trámites para garantizar la trazabilidad.
 */
@Service
@RequiredArgsConstructor
public class HistorialEstadoService {

    private final HistorialEstadoRepository historialEstadoRepository;
    private final TramiteRepository tramiteRepository;
    private final UsuarioRepository usuarioRepository;

    // Listar todos los registros de historial
    public List<HistorialEstadoResponseDTO> listarTodos() {
        return historialEstadoRepository.findAll()
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    // Buscar un registro por ID
    public Optional<HistorialEstadoResponseDTO> buscarPorId(Long id) {
        return historialEstadoRepository.findById(id)
                .map(this::convertirAResponse);
    }

    // Guardar un nuevo registro de cambio de estado
    public HistorialEstadoResponseDTO guardar(HistorialEstadoRequestDTO dto) {
        Tramite tramite = tramiteRepository.findById(dto.getTramiteId())
                .orElseThrow(() -> new RuntimeException("Trámite no encontrado"));

        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        HistorialEstado historial = new HistorialEstado();
        historial.setTramite(tramite);
        historial.setUsuario(usuario);
        historial.setEstadoAnterior(dto.getEstadoAnterior());
        historial.setEstadoNuevo(dto.getEstadoNuevo());
        historial.setFechaCambio(LocalDate.now());
        historial.setObservacion(dto.getObservacion());

        HistorialEstado guardado = historialEstadoRepository.save(historial);
        return convertirAResponse(guardado);
    }

    // Eliminar un registro por ID
    public void eliminar(Long id) {
        historialEstadoRepository.deleteById(id);
    }

    // Convertir entidad a DTO de respuesta
    private HistorialEstadoResponseDTO convertirAResponse(HistorialEstado historial) {
        HistorialEstadoResponseDTO dto = new HistorialEstadoResponseDTO();
        dto.setHistorialId(historial.getHistorialId());
        dto.setTramiteId(historial.getTramite().getTramiteId());
        dto.setEstadoAnterior(historial.getEstadoAnterior());
        dto.setEstadoNuevo(historial.getEstadoNuevo());
        dto.setNombreUsuario(historial.getUsuario().getNombre());
        dto.setFechaCambio(historial.getFechaCambio());
        dto.setObservacion(historial.getObservacion());
        return dto;
    }
}