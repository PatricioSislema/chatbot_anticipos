package ec.edu.iti.anticipos.chatbot.service;

import ec.edu.iti.anticipos.chatbot.dto.request.TramiteRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.TramiteResponseDTO;
import ec.edu.iti.anticipos.chatbot.entity.TipoTramite;
import ec.edu.iti.anticipos.chatbot.entity.Tramite;
import ec.edu.iti.anticipos.chatbot.entity.Usuario;
import ec.edu.iti.anticipos.chatbot.repository.TipoTramiteRepository;
import ec.edu.iti.anticipos.chatbot.repository.TramiteRepository;
import ec.edu.iti.anticipos.chatbot.repository.UsuarioRepository;
import ec.edu.iti.anticipos.chatbot.repository.OrdenTransferenciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final OrdenTransferenciaRepository ordenTransferenciaRepository;

    // Listar todos los trámites (Agregado @Transactional para evitar el LazyLoading)
    @Transactional(readOnly = true)
    public List<TramiteResponseDTO> listarTodos() {
        return tramiteRepository.findAll()
                .stream()
                .map(tramite -> {
                    if (tramite.getUsuario() != null) {
                        tramite.getUsuario().getTelefono();
                    }
                    return convertirAResponse(tramite);
                })
                .toList();
    }

    public Optional<TramiteResponseDTO> buscarPorId(Long id) {
        return tramiteRepository.findById(id).map(this::convertirAResponse);
    }

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

    public TramiteResponseDTO actualizarEstado(Long id, String nuevoEstado) {
        Tramite tramite = tramiteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trámite no encontrado"));

        tramite.setEstado(nuevoEstado);
        Tramite actualizado = tramiteRepository.save(tramite);
        return convertirAResponse(actualizado);
    }

    public void eliminar(Long id) {
        tramiteRepository.deleteById(id);
    }

    // ==========================================
    // CONVERSIÓN A DTO
    // ==========================================
    private TramiteResponseDTO convertirAResponse(Tramite tramite) {
        TramiteResponseDTO dto = new TramiteResponseDTO();
        dto.setTramiteId(tramite.getTramiteId());
        dto.setEstado(tramite.getEstado());
        dto.setFechaCreacion(tramite.getFechaCreacion());
        dto.setNombreUsuario(tramite.getUsuario().getNombre());
        dto.setNombreTipoTramite(tramite.getTipoTramite().getNombre());
        dto.setCorreoUsuario(tramite.getUsuario().getCorreo());
        dto.setTelefono(tramite.getUsuario().getTelefono());

        // 1. Presupuesto y Propósito
        Optional<ec.edu.iti.anticipos.chatbot.entity.Solicitud> solicitudOpt = solicitudRepository.findByTramiteTramiteId(tramite.getTramiteId());

        if (solicitudOpt.isPresent()) {
            dto.setPresupuesto(solicitudOpt.get().getPresupuesto());
            dto.setProposito(solicitudOpt.get().getProposito());
        } else {
            liquidacionRepository.findByTramiteTramiteId(tramite.getTramiteId()).ifPresentOrElse(
                    liquidacion -> {
                        dto.setPresupuesto(liquidacion.getTotalGastado());
                        dto.setProposito("Liquidación de Anticipo");
                    },
                    () -> dto.setProposito("Liquidación de Anticipo")
            );
        }

        // 2. Fecha de Transferencia (Lógica Relacional a prueba de fallos)
        try {
            Optional<ec.edu.iti.anticipos.chatbot.entity.Liquidacion> liqOpt = liquidacionRepository.findByTramiteTramiteId(tramite.getTramiteId());

            if (liqOpt.isPresent()) {
                // Si es una liquidación, intentamos buscar la orden del anticipo original
                if (liqOpt.get().getTramiteOriginal() != null) {
                    List<ec.edu.iti.anticipos.chatbot.entity.OrdenTransferencia> ordenes = ordenTransferenciaRepository.findByTramiteTramiteId(liqOpt.get().getTramiteOriginal().getTramiteId());
                    if (!ordenes.isEmpty()) {
                        dto.setFechaTransferencia(ordenes.get(0).getFechaGeneracion());
                    }
                }
                // SI FALLA O NO HAY ORIGINAL, usamos la fecha de creación para no dejarlo en blanco
                if (dto.getFechaTransferencia() == null) {
                    dto.setFechaTransferencia(tramite.getFechaCreacion());
                }
            } else {
                // Es un Anticipo directo. Buscamos su propia orden.
                List<ec.edu.iti.anticipos.chatbot.entity.OrdenTransferencia> ordenes = ordenTransferenciaRepository.findByTramiteTramiteId(tramite.getTramiteId());
                if (!ordenes.isEmpty()) {
                    dto.setFechaTransferencia(ordenes.get(0).getFechaGeneracion());
                }
            }
        } catch (Exception e) {
            System.err.println("Error buscando fecha para trámite " + tramite.getTramiteId() + ": " + e.getMessage());
        }

        return dto;
    }

    public List<TramiteResponseDTO> listarPorEstado(String estado) {
        return tramiteRepository.findByEstado(estado).stream().map(this::convertirAResponse).toList();
    }

    public List<TramiteResponseDTO> listarPorUsuario(Long usuarioId) {
        return tramiteRepository.findByUsuarioUsuarioId(usuarioId).stream().map(this::convertirAResponse).toList();
    }

    public Optional<TramiteDetalleDTO> obtenerDetalle(Long tramiteId) {
        return tramiteRepository.findById(tramiteId).map(tramite -> {
            TramiteDetalleDTO dto = new TramiteDetalleDTO();
            dto.setTramiteId(tramite.getTramiteId());
            dto.setEstado(tramite.getEstado());
            dto.setFechaCreacion(tramite.getFechaCreacion());
            dto.setNombreTipoTramite(tramite.getTipoTramite().getNombre());

            if (tramite.getUsuario() != null) {
                dto.setUsuarioId(tramite.getUsuario().getUsuarioId());
                dto.setNombreUsuario(tramite.getUsuario().getNombre());
                dto.setCorreoUsuario(tramite.getUsuario().getCorreo());
                dto.setCedulaUsuario(tramite.getUsuario().getCedula());
            }

            solicitudRepository.findByTramiteTramiteId(tramiteId).ifPresent(solicitud -> {
                dto.setUnidad(solicitud.getUnidad());
                dto.setCampus(solicitud.getCampus());
                dto.setProposito(solicitud.getProposito());
                dto.setLugar(solicitud.getLugar());
                dto.setFechaRealizacion(solicitud.getFechaRealizacion());
                dto.setPresupuesto(solicitud.getPresupuesto());
            });

            liquidacionRepository.findByTramiteTramiteId(tramiteId).ifPresent(liquidacion -> {
                dto.setTotalGastado(liquidacion.getTotalGastado());
                dto.setSaldo(liquidacion.getSaldo());
                dto.setFechaPresentacion(liquidacion.getFechaPresentacion());
            });

            return dto;
        });
    }
}