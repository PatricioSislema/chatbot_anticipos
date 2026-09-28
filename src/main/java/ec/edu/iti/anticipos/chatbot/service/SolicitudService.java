package ec.edu.iti.anticipos.chatbot.service;

import ec.edu.iti.anticipos.chatbot.dto.request.SolicitudRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.SolicitudResponseDTO;
import ec.edu.iti.anticipos.chatbot.entity.Solicitud;
import ec.edu.iti.anticipos.chatbot.entity.Tramite;
import ec.edu.iti.anticipos.chatbot.repository.SolicitudRepository;
import ec.edu.iti.anticipos.chatbot.repository.TramiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Servicio para la entidad Solicitud.
 * Contiene la lógica de negocio relacionada con las solicitudes de anticipo.
 */
@Service
@RequiredArgsConstructor
public class SolicitudService {

    private final SolicitudRepository solicitudRepository;
    private final TramiteRepository tramiteRepository;

    // Listar todas las solicitudes
    public List<SolicitudResponseDTO> listarTodos() {
        return solicitudRepository.findAll()
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    // Buscar una solicitud por ID
    public Optional<SolicitudResponseDTO> buscarPorId(Long id) {
        return solicitudRepository.findById(id)
                .map(this::convertirAResponse);
    }

    // Buscar la solicitud de un usuario que esté pendiente de liquidar
    public Optional<SolicitudResponseDTO> buscarPorUsuario(Long usuarioId) {
        return solicitudRepository.findByTramiteUsuarioUsuarioId(usuarioId)
                .stream()
                .findFirst()
                .map(this::convertirAResponse);
    }

    // Buscar la solicitud de un usuario que esté en estado "Transferido"
    public Optional<SolicitudResponseDTO> buscarAnticipoTransferido(Long usuarioId) {
        return solicitudRepository
                .findByTramiteUsuarioUsuarioIdAndTramiteEstado(usuarioId, "Transferido")
                .stream()
                .findFirst()
                .map(this::convertirAResponse);
    }

    // Guardar una nueva solicitud
    public SolicitudResponseDTO guardar(SolicitudRequestDTO dto) {
        Tramite tramite = tramiteRepository.findById(dto.getTramiteId())
                .orElseThrow(() -> new RuntimeException("Trámite no encontrado"));

        Solicitud solicitud = new Solicitud();
        solicitud.setTramite(tramite);
        solicitud.setUnidad(dto.getUnidad());
        solicitud.setCampus(dto.getCampus());
        solicitud.setProposito(dto.getProposito());
        solicitud.setLugar(dto.getLugar());
        solicitud.setFechaRealizacion(dto.getFechaRealizacion());
        solicitud.setPresupuesto(dto.getPresupuesto());
        solicitud.setFirma(dto.getFirma());

        Solicitud guardada = solicitudRepository.save(solicitud);
        return convertirAResponse(guardada);
    }

    // Eliminar una solicitud por ID
    public void eliminar(Long id) {
        solicitudRepository.deleteById(id);
    }

    // Convertir entidad a DTO de respuesta
    private SolicitudResponseDTO convertirAResponse(Solicitud solicitud) {
        SolicitudResponseDTO dto = new SolicitudResponseDTO();
        dto.setSolicitudId(solicitud.getSolicitudId());
        dto.setTramiteId(solicitud.getTramite().getTramiteId());
        dto.setUnidad(solicitud.getUnidad());
        dto.setCampus(solicitud.getCampus());
        dto.setProposito(solicitud.getProposito());
        dto.setLugar(solicitud.getLugar());
        dto.setFechaRealizacion(solicitud.getFechaRealizacion());
        dto.setPresupuesto(solicitud.getPresupuesto());
        dto.setFirma(solicitud.getFirma());
        return dto;
    }
}