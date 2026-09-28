package ec.edu.iti.anticipos.chatbot.service;

import ec.edu.iti.anticipos.chatbot.dto.request.DevolucionRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.DevolucionResponseDTO;
import ec.edu.iti.anticipos.chatbot.entity.Devolucion;
import ec.edu.iti.anticipos.chatbot.entity.Liquidacion;
import ec.edu.iti.anticipos.chatbot.repository.DevolucionRepository;
import ec.edu.iti.anticipos.chatbot.repository.LiquidacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Servicio para la entidad Devolucion.
 * Contiene la lógica de negocio relacionada con la devolución de excedentes.
 */
@Service
@RequiredArgsConstructor
public class DevolucionService {

    private final DevolucionRepository devolucionRepository;
    private final LiquidacionRepository liquidacionRepository;

    // Listar todas las devoluciones
    public List<DevolucionResponseDTO> listarTodos() {
        return devolucionRepository.findAll()
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    // Buscar una devolución por ID
    public Optional<DevolucionResponseDTO> buscarPorId(Long id) {
        return devolucionRepository.findById(id)
                .map(this::convertirAResponse);
    }

    public Optional<DevolucionResponseDTO> buscarPendientePorUsuario(Long usuarioId) {
        // Buscar devoluciones sin comprobante de un usuario específico
        return devolucionRepository.findAll()
                .stream()
                .filter(d -> d.getComprobantePdf() == null || d.getComprobantePdf().isEmpty())
                .filter(d -> d.getLiquidacion().getTramite().getUsuario().getUsuarioId().equals(usuarioId))
                .findFirst()
                .map(this::convertirAResponse);
    }

    // Buscar devolución pendiente por liquidación
    public Optional<DevolucionResponseDTO> buscarPendientePorLiquidacion(Long liquidacionId) {
        return devolucionRepository.findByLiquidacionLiquidacionId(liquidacionId)
                .stream()
                .filter(d -> d.getComprobantePdf() == null || d.getComprobantePdf().isEmpty())
                .findFirst()
                .map(this::convertirAResponse);
    }

    // Guardar una nueva devolución
    public DevolucionResponseDTO guardar(DevolucionRequestDTO dto) {
        Liquidacion liquidacion = liquidacionRepository.findById(dto.getLiquidacionId())
                .orElseThrow(() -> new RuntimeException("Liquidación no encontrada"));

        Devolucion devolucion = new Devolucion();
        devolucion.setLiquidacion(liquidacion);
        devolucion.setMonto(dto.getMonto());
        devolucion.setFechaDevolucion(dto.getFechaDevolucion());
        devolucion.setComprobantePdf(dto.getComprobantePdf());

        Devolucion guardada = devolucionRepository.save(devolucion);
        return convertirAResponse(guardada);
    }

    // Actualizar una devolución existente
    public DevolucionResponseDTO actualizar(Long id, DevolucionRequestDTO dto) {
        Devolucion devolucion = devolucionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Devolución no encontrada"));

        devolucion.setMonto(dto.getMonto());
        devolucion.setFechaDevolucion(dto.getFechaDevolucion());
        devolucion.setComprobantePdf(dto.getComprobantePdf());

        Devolucion actualizada = devolucionRepository.save(devolucion);
        return convertirAResponse(actualizada);
    }

    // Eliminar una devolución por ID
    public void eliminar(Long id) {
        devolucionRepository.deleteById(id);
    }

    // Convertir entidad a DTO de respuesta
    private DevolucionResponseDTO convertirAResponse(Devolucion devolucion) {
        DevolucionResponseDTO dto = new DevolucionResponseDTO();
        dto.setDevolucionId(devolucion.getDevolucionId());
        dto.setLiquidacionId(devolucion.getLiquidacion().getLiquidacionId());
        dto.setTramiteId(devolucion.getLiquidacion().getTramite().getTramiteId());
        dto.setMonto(devolucion.getMonto());
        dto.setFechaDevolucion(devolucion.getFechaDevolucion());
        dto.setComprobantePdf(devolucion.getComprobantePdf());
        return dto;
    }
}
