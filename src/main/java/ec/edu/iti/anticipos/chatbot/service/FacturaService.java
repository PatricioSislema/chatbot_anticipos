package ec.edu.iti.anticipos.chatbot.service;

import ec.edu.iti.anticipos.chatbot.dto.request.FacturaRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.FacturaResponseDTO;
import ec.edu.iti.anticipos.chatbot.entity.Factura;
import ec.edu.iti.anticipos.chatbot.entity.Liquidacion;
import ec.edu.iti.anticipos.chatbot.repository.FacturaRepository;
import ec.edu.iti.anticipos.chatbot.repository.LiquidacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Servicio para la entidad Factura.
 * Contiene la lógica de negocio relacionada con las facturas subidas durante la liquidación.
 */
@Service
@RequiredArgsConstructor
public class FacturaService {

    private final FacturaRepository facturaRepository;
    private final LiquidacionRepository liquidacionRepository;

    // Listar todas las facturas
    public List<FacturaResponseDTO> listarTodos() {
        return facturaRepository.findAll()
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    // Buscar una factura por ID
    public Optional<FacturaResponseDTO> buscarPorId(Long id) {
        return facturaRepository.findById(id)
                .map(this::convertirAResponse);
    }

    // Guardar una nueva factura
    public FacturaResponseDTO guardar(FacturaRequestDTO dto) {
        Liquidacion liquidacion = liquidacionRepository.findById(dto.getLiquidacionId())
                .orElseThrow(() -> new RuntimeException("Liquidación no encontrada"));

        Factura factura = new Factura();
        factura.setLiquidacion(liquidacion);
        factura.setNumeroFactura(dto.getNumeroFactura());
        factura.setProveedor(dto.getProveedor());
        factura.setRuc(dto.getRuc());
        factura.setFecha(dto.getFecha());
        factura.setConcepto(dto.getConcepto());
        factura.setMonto(dto.getMonto());
        factura.setArchivoPdf(dto.getArchivoPdf());

        Factura guardada = facturaRepository.save(factura);
        return convertirAResponse(guardada);
    }

    // Eliminar una factura por ID
    public void eliminar(Long id) {
        facturaRepository.deleteById(id);
    }

    // Convertir entidad a DTO de respuesta
    private FacturaResponseDTO convertirAResponse(Factura factura) {
        FacturaResponseDTO dto = new FacturaResponseDTO();
        dto.setFacturaId(factura.getFacturaId());
        dto.setLiquidacionId(factura.getLiquidacion().getLiquidacionId());
        dto.setNumeroFactura(factura.getNumeroFactura());
        dto.setProveedor(factura.getProveedor());
        dto.setRuc(factura.getRuc());
        dto.setFecha(factura.getFecha());
        dto.setConcepto(factura.getConcepto());
        dto.setMonto(factura.getMonto());
        dto.setArchivoPdf(factura.getArchivoPdf());
        return dto;
    }
}