package ec.edu.iti.anticipos.chatbot.service;

import ec.edu.iti.anticipos.chatbot.dto.request.ReembolsoRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.ReembolsoResponseDTO;
import ec.edu.iti.anticipos.chatbot.entity.Liquidacion;
import ec.edu.iti.anticipos.chatbot.entity.Reembolso;
import ec.edu.iti.anticipos.chatbot.entity.Usuario;
import ec.edu.iti.anticipos.chatbot.repository.LiquidacionRepository;
import ec.edu.iti.anticipos.chatbot.repository.ReembolsoRepository;
import ec.edu.iti.anticipos.chatbot.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Servicio para la entidad Reembolso.
 * Contiene la lógica de negocio relacionada con los reembolsos
 * cuando el funcionario gastó más de lo anticipado (caso FALTANTE).
 *
 * FLUJO FALTANTE:
 *  1. Contadora aprueba liquidación → crea Reembolso (Pendiente)
 *  2. Director autoriza → Reembolso (Autorizado)
 *  3. Contadora informa a Colectora → Colectora genera orden
 *  4. Director aprueba la orden → dinero llega al Funcionario
 *  5. Contadora cierra la liquidación
 */
@Service
@RequiredArgsConstructor
public class ReembolsoService {

    private final ReembolsoRepository reembolsoRepository;
    private final LiquidacionRepository liquidacionRepository;
    private final UsuarioRepository usuarioRepository;

    // Listar todos los reembolsos
    public List<ReembolsoResponseDTO> listarTodos() {
        return reembolsoRepository.findAll()
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    // Buscar un reembolso por ID
    public Optional<ReembolsoResponseDTO> buscarPorId(Long id) {
        return reembolsoRepository.findById(id)
                .map(this::convertirAResponse);
    }

    // Buscar el reembolso de una liquidación específica
    public Optional<ReembolsoResponseDTO> buscarPorLiquidacion(Long liquidacionId) {
        return reembolsoRepository.findByLiquidacionLiquidacionId(liquidacionId)
                .map(this::convertirAResponse);
    }

    // ✅ NUEVO: Buscar el reembolso de un trámite específico
    public Optional<ReembolsoResponseDTO> buscarPorTramite(Long tramiteId) {
        return reembolsoRepository.findAll()
                .stream()
                .filter(r -> r.getLiquidacion() != null
                        && r.getLiquidacion().getTramite() != null
                        && r.getLiquidacion().getTramite().getTramiteId().equals(tramiteId))
                .findFirst()
                .map(this::convertirAResponse);
    }

    // Listar reembolsos por estado
    public List<ReembolsoResponseDTO> listarPorEstado(String estado) {
        return reembolsoRepository.findByEstado(estado)
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    // Guardar un nuevo reembolso
    public ReembolsoResponseDTO guardar(ReembolsoRequestDTO dto) {
        Liquidacion liquidacion = liquidacionRepository.findById(dto.getLiquidacionId())
                .orElseThrow(() -> new RuntimeException("Liquidación no encontrada"));

        Reembolso reembolso = new Reembolso();
        reembolso.setLiquidacion(liquidacion);
        reembolso.setMontoSolicitado(dto.getMontoSolicitado());
        reembolso.setFechaSolicitud(dto.getFechaSolicitud() != null ? dto.getFechaSolicitud() : LocalDate.now());
        reembolso.setFechaAutorizacion(dto.getFechaAutorizacion());
        reembolso.setEstado(dto.getEstado() != null ? dto.getEstado() : "Pendiente");
        reembolso.setObservacion(dto.getObservacion());

        if (dto.getAutorizadoPor() != null) {
            Usuario director = usuarioRepository.findById(dto.getAutorizadoPor())
                    .orElseThrow(() -> new RuntimeException("Usuario autorizador no encontrado"));
            reembolso.setAutorizadoPor(director);
        }

        Reembolso guardado = reembolsoRepository.save(reembolso);
        return convertirAResponse(guardado);
    }

    // Actualizar un reembolso existente
    public ReembolsoResponseDTO actualizar(Long id, ReembolsoRequestDTO dto) {
        Reembolso reembolso = reembolsoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reembolso no encontrado"));

        reembolso.setMontoSolicitado(dto.getMontoSolicitado());
        reembolso.setFechaSolicitud(dto.getFechaSolicitud());
        reembolso.setFechaAutorizacion(dto.getFechaAutorizacion());
        reembolso.setEstado(dto.getEstado());
        reembolso.setObservacion(dto.getObservacion());

        if (dto.getAutorizadoPor() != null) {
            Usuario director = usuarioRepository.findById(dto.getAutorizadoPor())
                    .orElseThrow(() -> new RuntimeException("Usuario autorizador no encontrado"));
            reembolso.setAutorizadoPor(director);
        }

        Reembolso actualizado = reembolsoRepository.save(reembolso);
        return convertirAResponse(actualizado);
    }

    // ✅ NUEVO: Autorizar un reembolso (lo hace el Director Financiero)
    public ReembolsoResponseDTO autorizar(Long reembolsoId, Long directorId, String observacion) {
        Reembolso reembolso = reembolsoRepository.findById(reembolsoId)
                .orElseThrow(() -> new RuntimeException("Reembolso no encontrado"));

        if (!"Pendiente".equals(reembolso.getEstado())) {
            throw new RuntimeException("El reembolso no está en estado Pendiente. Estado actual: " + reembolso.getEstado());
        }

        Usuario director = usuarioRepository.findById(directorId)
                .orElseThrow(() -> new RuntimeException("Director no encontrado"));

        reembolso.setEstado("Autorizado");
        reembolso.setFechaAutorizacion(LocalDate.now());
        reembolso.setAutorizadoPor(director);
        if (observacion != null && !observacion.isEmpty()) {
            reembolso.setObservacion(observacion);
        }

        Reembolso guardado = reembolsoRepository.save(reembolso);
        return convertirAResponse(guardado);
    }

    // ✅ NUEVO: Cerrar el reembolso (lo hace la Contadora tras la transferencia)
    public ReembolsoResponseDTO cerrar(Long reembolsoId) {
        Reembolso reembolso = reembolsoRepository.findById(reembolsoId)
                .orElseThrow(() -> new RuntimeException("Reembolso no encontrado"));

        reembolso.setEstado("Cerrado");
        Reembolso guardado = reembolsoRepository.save(reembolso);
        return convertirAResponse(guardado);
    }

    // Eliminar un reembolso por ID
    public void eliminar(Long id) {
        reembolsoRepository.deleteById(id);
    }

    // Convertir entidad a DTO de respuesta
    private ReembolsoResponseDTO convertirAResponse(Reembolso reembolso) {
        ReembolsoResponseDTO dto = new ReembolsoResponseDTO();
        dto.setReembolsoId(reembolso.getReembolsoId());
        dto.setLiquidacionId(reembolso.getLiquidacion().getLiquidacionId());
        dto.setTramiteId(reembolso.getLiquidacion().getTramite().getTramiteId());
        dto.setMontoSolicitado(reembolso.getMontoSolicitado());
        dto.setFechaSolicitud(reembolso.getFechaSolicitud());
        dto.setFechaAutorizacion(reembolso.getFechaAutorizacion());
        dto.setEstado(reembolso.getEstado());
        dto.setObservacion(reembolso.getObservacion());

        if (reembolso.getAutorizadoPor() != null) {
            dto.setAutorizadoPorId(reembolso.getAutorizadoPor().getUsuarioId());
            dto.setAutorizadoPorNombre(reembolso.getAutorizadoPor().getNombre());
        }

        return dto;
    }
}