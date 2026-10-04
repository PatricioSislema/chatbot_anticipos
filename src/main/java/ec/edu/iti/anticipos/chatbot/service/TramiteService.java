package ec.edu.iti.anticipos.chatbot.service;

import ec.edu.iti.anticipos.chatbot.dto.request.TramiteRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.TramiteResponseDTO;
import ec.edu.iti.anticipos.chatbot.entity.TipoTramite;
import ec.edu.iti.anticipos.chatbot.entity.Tramite;
import ec.edu.iti.anticipos.chatbot.entity.Usuario;
import ec.edu.iti.anticipos.chatbot.repository.TipoTramiteRepository;
import ec.edu.iti.anticipos.chatbot.repository.TramiteRepository;
import ec.edu.iti.anticipos.chatbot.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ec.edu.iti.anticipos.chatbot.dto.response.TramiteDetalleDTO;
import ec.edu.iti.anticipos.chatbot.repository.SolicitudRepository;
import ec.edu.iti.anticipos.chatbot.repository.LiquidacionRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Servicio para la entidad Tramite.
 * Contiene la lógica de negocio relacionada con los trámites.
 */
@Service
@RequiredArgsConstructor
public class TramiteService {

    private final TramiteRepository tramiteRepository;
    private final UsuarioRepository usuarioRepository;
    private final TipoTramiteRepository tipoTramiteRepository;
    private final SolicitudRepository solicitudRepository;
    private final LiquidacionRepository liquidacionRepository;

    // Listar todos los trámites
    public List<TramiteResponseDTO> listarTodos() {
        return tramiteRepository.findAll()
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    // Buscar un trámite por ID
    public Optional<TramiteResponseDTO> buscarPorId(Long id) {
        return tramiteRepository.findById(id)
                .map(this::convertirAResponse);
    }

    // Guardar un nuevo trámite
    public TramiteResponseDTO guardar(TramiteRequestDTO dto) {
        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        TipoTramite tipoTramite = tipoTramiteRepository.findById(dto.getTipoTramiteId())
                .orElseThrow(() -> new RuntimeException("Tipo de trámite no encontrado"));

        Tramite tramite = new Tramite();
        tramite.setUsuario(usuario);
        tramite.setTipoTramite(tipoTramite);
        tramite.setEstado(dto.getEstado() != null ? dto.getEstado() : "Pendiente");
        tramite.setFechaCreacion(LocalDate.now());

        Tramite guardado = tramiteRepository.save(tramite);
        return convertirAResponse(guardado);
    }

    // Actualizar el estado de un trámite
    public TramiteResponseDTO actualizarEstado(Long id, String nuevoEstado) {
        Tramite tramite = tramiteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trámite no encontrado"));

        tramite.setEstado(nuevoEstado);
        Tramite actualizado = tramiteRepository.save(tramite);
        return convertirAResponse(actualizado);
    }

    // Eliminar un trámite por ID
    public void eliminar(Long id) {
        tramiteRepository.deleteById(id);
    }

    // Convertir entidad a DTO de respuesta
    private TramiteResponseDTO convertirAResponse(Tramite tramite) {
        TramiteResponseDTO dto = new TramiteResponseDTO();
        dto.setTramiteId(tramite.getTramiteId());
        dto.setEstado(tramite.getEstado());
        dto.setFechaCreacion(tramite.getFechaCreacion());
        dto.setNombreUsuario(tramite.getUsuario().getNombre());
        dto.setNombreTipoTramite(tramite.getTipoTramite().getNombre());
        return dto;
    }

    // Listar trámites por estado
    public List<TramiteResponseDTO> listarPorEstado(String estado) {
        return tramiteRepository.findByEstado(estado)
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    // Listar trámites por usuario
    public List<TramiteResponseDTO> listarPorUsuario(Long usuarioId) {
        return tramiteRepository.findByUsuarioUsuarioId(usuarioId)
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    // Obtener el detalle completo de un trámite
    public Optional<TramiteDetalleDTO> obtenerDetalle(Long tramiteId) {
        return tramiteRepository.findById(tramiteId).map(tramite -> {
            TramiteDetalleDTO dto = new TramiteDetalleDTO();

            // Datos del trámite
            dto.setTramiteId(tramite.getTramiteId());
            dto.setEstado(tramite.getEstado());
            dto.setFechaCreacion(tramite.getFechaCreacion());
            dto.setNombreTipoTramite(tramite.getTipoTramite().getNombre());

            // Datos del usuario
            if (tramite.getUsuario() != null) {
                dto.setUsuarioId(tramite.getUsuario().getUsuarioId());
                dto.setNombreUsuario(tramite.getUsuario().getNombre());
                dto.setCorreoUsuario(tramite.getUsuario().getCorreo());
                dto.setCedulaUsuario(tramite.getUsuario().getCedula());
            }

            // Datos de la solicitud (si existe)
            solicitudRepository.findByTramiteTramiteId(tramiteId).ifPresent(solicitud -> {
                dto.setUnidad(solicitud.getUnidad());
                dto.setCampus(solicitud.getCampus());
                dto.setProposito(solicitud.getProposito());
                dto.setLugar(solicitud.getLugar());
                dto.setFechaRealizacion(solicitud.getFechaRealizacion());
                dto.setPresupuesto(solicitud.getPresupuesto());
            });

            // Datos de la liquidación (si existe)
            liquidacionRepository.findByTramiteTramiteId(tramiteId).ifPresent(liquidacion -> {
                dto.setTotalGastado(liquidacion.getTotalGastado());
                dto.setSaldo(liquidacion.getSaldo());
                dto.setFechaPresentacion(liquidacion.getFechaPresentacion());
            });

            return dto;
        });
    }
}
