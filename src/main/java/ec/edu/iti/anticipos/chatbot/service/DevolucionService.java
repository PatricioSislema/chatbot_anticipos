package ec.edu.iti.anticipos.chatbot.service;

import ec.edu.iti.anticipos.chatbot.dto.ComprobanteExtraidoDTO;
import ec.edu.iti.anticipos.chatbot.dto.request.DevolucionRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.DevolucionResponseDTO;
import ec.edu.iti.anticipos.chatbot.entity.Devolucion;
import ec.edu.iti.anticipos.chatbot.entity.Liquidacion;
import ec.edu.iti.anticipos.chatbot.repository.DevolucionRepository;
import ec.edu.iti.anticipos.chatbot.repository.LiquidacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Servicio para la entidad Devolucion.
 * Contiene la lógica de negocio relacionada con la devolución de excedentes.
 * Incluye el manejo del estado: Pendiente, Verificado o Rechazado.
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

    // Buscar devoluciones pendientes por usuario
    public Optional<DevolucionResponseDTO> buscarPendientePorUsuario(Long usuarioId) {
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
        devolucion.setEstado(dto.getEstado() != null ? dto.getEstado() : "Pendiente");

        // NUEVOS campos de auditoría (si vienen en el request)
        devolucion.setBancoOrigen(dto.getBancoOrigen());
        devolucion.setCuentaOrigen(dto.getCuentaOrigen());
        devolucion.setMontoDepositado(dto.getMontoDepositado());
        devolucion.setFechaDeposito(dto.getFechaDeposito());

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

        // NUEVOS campos de auditoría
        if (dto.getBancoOrigen() != null) devolucion.setBancoOrigen(dto.getBancoOrigen());
        if (dto.getCuentaOrigen() != null) devolucion.setCuentaOrigen(dto.getCuentaOrigen());
        if (dto.getMontoDepositado() != null) devolucion.setMontoDepositado(dto.getMontoDepositado());
        if (dto.getFechaDeposito() != null) devolucion.setFechaDeposito(dto.getFechaDeposito());

        if (dto.getComprobantePdf() != null && !dto.getComprobantePdf().isEmpty()) {
            devolucion.setEstado("Comprobante subido");
        }

        Devolucion actualizada = devolucionRepository.save(devolucion);
        return convertirAResponse(actualizada);
    }

    // Actualizar solo el estado de una devolución
    public DevolucionResponseDTO actualizarEstado(Long id, String nuevoEstado) {
        Devolucion devolucion = devolucionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Devolución no encontrada"));
        devolucion.setEstado(nuevoEstado);
        Devolucion actualizada = devolucionRepository.save(devolucion);
        return convertirAResponse(actualizada);
    }

    // Listar devoluciones por estado
    public List<DevolucionResponseDTO> listarPorEstado(String estado) {
        return devolucionRepository.findAll()
                .stream()
                .filter(d -> estado.equals(d.getEstado()))
                .map(this::convertirAResponse)
                .toList();
    }

    // Buscar una devolución por ID de trámite
    public Optional<DevolucionResponseDTO> buscarPorTramite(Long tramiteId) {
        return devolucionRepository.findAll()
                .stream()
                .filter(d -> d.getLiquidacion().getTramite().getTramiteId().equals(tramiteId))
                .findFirst()
                .map(this::convertirAResponse);
    }

    // Eliminar una devolución por ID
    public void eliminar(Long id) {
        devolucionRepository.deleteById(id);
    }

    // ============================================================
    // NUEVOS MÉTODOS para validación de comprobantes digitales
    // ============================================================

    /**
     * Obtiene el saldo pendiente a devolver (el saldo de la liquidación).
     */
    public BigDecimal obtenerSaldoPendiente(Long devolucionId) {
        Devolucion d = devolucionRepository.findById(devolucionId)
                .orElseThrow(() -> new RuntimeException("Devolución no encontrada: " + devolucionId));

        Liquidacion liq = d.getLiquidacion();
        if (liq == null) {
            throw new RuntimeException("La devolución no tiene liquidación asociada");
        }
        return liq.getSaldo();
    }

    /**
     * Guarda los datos extraídos del comprobante digital en la devolución.
     */
    public void guardarDatosComprobante(Long devolucionId, ComprobanteExtraidoDTO extraido) {
        Devolucion d = devolucionRepository.findById(devolucionId)
                .orElseThrow(() -> new RuntimeException("Devolución no encontrada: " + devolucionId));

        d.setBancoOrigen(extraido.getBancoOrigen());
        d.setCuentaOrigen(extraido.getCuentaOrigen());
        d.setMontoDepositado(extraido.getMontoDepositado());
        d.setFechaDeposito(extraido.getFechaDeposito());

        devolucionRepository.save(d);
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
        dto.setEstado(devolucion.getEstado());

        // NUEVOS campos de auditoría
        dto.setBancoOrigen(devolucion.getBancoOrigen());
        dto.setCuentaOrigen(devolucion.getCuentaOrigen());
        dto.setMontoDepositado(devolucion.getMontoDepositado());
        dto.setFechaDeposito(devolucion.getFechaDeposito());

        return dto;
    }
}