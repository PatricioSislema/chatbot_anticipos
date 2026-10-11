package ec.edu.iti.anticipos.chatbot.service;

import ec.edu.iti.anticipos.chatbot.dto.request.LiquidacionRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.LiquidacionResponseDTO;
import ec.edu.iti.anticipos.chatbot.entity.Liquidacion;
import ec.edu.iti.anticipos.chatbot.entity.Tramite;
import ec.edu.iti.anticipos.chatbot.repository.LiquidacionRepository;
import ec.edu.iti.anticipos.chatbot.repository.TramiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Servicio para la entidad Liquidacion.
 * Contiene la lógica de negocio relacionada con las liquidaciones de anticipo,
 * incluyendo el cálculo de saldos y la detección del tipo de caso.
 */
@Service
@RequiredArgsConstructor
public class LiquidacionService {

    private final LiquidacionRepository liquidacionRepository;
    private final TramiteRepository tramiteRepository;

    // Listar todas las liquidaciones
    public List<LiquidacionResponseDTO> listarTodos() {
        return liquidacionRepository.findAll()
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    // Buscar una liquidación por ID
    public Optional<LiquidacionResponseDTO> buscarPorId(Long id) {
        return liquidacionRepository.findById(id)
                .map(this::convertirAResponse);
    }

    // Buscar una liquidación por trámite
    public Optional<LiquidacionResponseDTO> buscarPorTramite(Long tramiteId) {
        return liquidacionRepository.findByTramiteTramiteId(tramiteId)
                .map(this::convertirAResponse);
    }

    // Guardar una nueva liquidación
    public LiquidacionResponseDTO guardar(LiquidacionRequestDTO dto) {
        Tramite tramite = tramiteRepository.findById(dto.getTramiteId())
                .orElseThrow(() -> new RuntimeException("Trámite no encontrado"));

        Liquidacion liquidacion = new Liquidacion();
        liquidacion.setTramite(tramite);
        liquidacion.setTotalGastado(dto.getTotalGastado());
        liquidacion.setSaldo(dto.getSaldo());
        liquidacion.setFechaPresentacion(dto.getFechaPresentacion());

        // ✅ Detectar y setear el tipo de caso automáticamente
        liquidacion.setTipoCaso(detectarTipoCaso(dto.getSaldo()));

        Liquidacion guardada = liquidacionRepository.save(liquidacion);
        return convertirAResponse(guardada);
    }

    // ✅ NUEVO: Detectar el tipo de caso según el saldo
    public String detectarTipoCaso(BigDecimal saldo) {
        if (saldo == null) return null;

        if (saldo.compareTo(BigDecimal.ZERO) > 0) {
            return "SOBRANTE";
        } else if (saldo.compareTo(BigDecimal.ZERO) < 0) {
            return "FALTANTE";
        } else {
            return "EXACTO";
        }
    }

    // ✅ NUEVO: Actualizar el tipo de caso de una liquidación existente
    public LiquidacionResponseDTO actualizarTipoCaso(Long liquidacionId, String tipoCaso) {
        Liquidacion liquidacion = liquidacionRepository.findById(liquidacionId)
                .orElseThrow(() -> new RuntimeException("Liquidación no encontrada"));
        liquidacion.setTipoCaso(tipoCaso);
        Liquidacion actualizada = liquidacionRepository.save(liquidacion);
        return convertirAResponse(actualizada);
    }

    // Calcular el saldo de una liquidación
    public BigDecimal calcularSaldo(BigDecimal anticipo, BigDecimal totalGastado) {
        return anticipo.subtract(totalGastado);
    }

    // Eliminar una liquidación por ID
    public void eliminar(Long id) {
        liquidacionRepository.deleteById(id);
    }

    // Convertir entidad a DTO de respuesta
    private LiquidacionResponseDTO convertirAResponse(Liquidacion liquidacion) {
        LiquidacionResponseDTO dto = new LiquidacionResponseDTO();
        dto.setLiquidacionId(liquidacion.getLiquidacionId());
        dto.setTramiteId(liquidacion.getTramite().getTramiteId());
        dto.setTotalGastado(liquidacion.getTotalGastado());
        dto.setSaldo(liquidacion.getSaldo());
        dto.setFechaPresentacion(liquidacion.getFechaPresentacion());
        dto.setTipoCaso(liquidacion.getTipoCaso());
        return dto;
    }
}