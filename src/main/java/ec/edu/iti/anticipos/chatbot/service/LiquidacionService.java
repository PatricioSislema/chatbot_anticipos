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
 * incluyendo el cálculo de saldos.
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

    // Guardar una nueva liquidación
    public LiquidacionResponseDTO guardar(LiquidacionRequestDTO dto) {
        Tramite tramite = tramiteRepository.findById(dto.getTramiteId())
                .orElseThrow(() -> new RuntimeException("Trámite no encontrado"));

        Liquidacion liquidacion = new Liquidacion();
        liquidacion.setTramite(tramite);
        liquidacion.setTotalGastado(dto.getTotalGastado());
        liquidacion.setSaldo(dto.getSaldo());
        liquidacion.setFechaPresentacion(dto.getFechaPresentacion());

        Liquidacion guardada = liquidacionRepository.save(liquidacion);
        return convertirAResponse(guardada);
    }

    // Calcular el saldo de una liquidación
    // saldo = anticipo - total gastado
    public BigDecimal calcularSaldo(BigDecimal anticipo, BigDecimal totalGastado) {
        return anticipo.subtract(totalGastado);
    }

    // Eliminar una liquidación por ID
    public void eliminar(Long id) {
        liquidacionRepository.deleteById(id);
    }
    public Optional<LiquidacionResponseDTO> buscarPorTramite(Long tramiteId) {
        return liquidacionRepository.findByTramiteTramiteId(tramiteId)
                .map(this::convertirAResponse);
    }

    // Convertir entidad a DTO de respuesta
    private LiquidacionResponseDTO convertirAResponse(Liquidacion liquidacion) {
        LiquidacionResponseDTO dto = new LiquidacionResponseDTO();
        dto.setLiquidacionId(liquidacion.getLiquidacionId());
        dto.setTramiteId(liquidacion.getTramite().getTramiteId());
        dto.setTotalGastado(liquidacion.getTotalGastado());
        dto.setSaldo(liquidacion.getSaldo());
        dto.setFechaPresentacion(liquidacion.getFechaPresentacion());
        return dto;
    }
}