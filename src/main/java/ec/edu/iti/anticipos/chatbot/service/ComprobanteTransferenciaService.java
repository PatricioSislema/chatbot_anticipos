package ec.edu.iti.anticipos.chatbot.service;

import ec.edu.iti.anticipos.chatbot.dto.request.ComprobanteTransferenciaRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.ComprobanteTransferenciaResponseDTO;
import ec.edu.iti.anticipos.chatbot.entity.ComprobanteTransferencia;
import ec.edu.iti.anticipos.chatbot.entity.Tramite;
import ec.edu.iti.anticipos.chatbot.repository.ComprobanteTransferenciaRepository;
import ec.edu.iti.anticipos.chatbot.repository.TramiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Servicio para la entidad ComprobanteTransferencia.
 * Contiene la lógica de negocio relacionada con los comprobantes
 * de transferencias (anticipo, devolución, reembolso).
 *
 * ⚠️ NOTA DE PROTOTIPO: por ahora solo se guarda el NOMBRE del archivo PDF.
 * En producción se debería usar Azure Blob Storage con nombres únicos (UUID).
 */
@Service
@RequiredArgsConstructor
public class ComprobanteTransferenciaService {

    private final ComprobanteTransferenciaRepository comprobanteTransferenciaRepository;
    private final TramiteRepository tramiteRepository;

    // Listar todos los comprobantes
    public List<ComprobanteTransferenciaResponseDTO> listarTodos() {
        return comprobanteTransferenciaRepository.findAll()
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    // Buscar un comprobante por ID
    public Optional<ComprobanteTransferenciaResponseDTO> buscarPorId(Long id) {
        return comprobanteTransferenciaRepository.findById(id)
                .map(this::convertirAResponse);
    }

    // Listar comprobantes de un trámite
    public List<ComprobanteTransferenciaResponseDTO> listarPorTramite(Long tramiteId) {
        return comprobanteTransferenciaRepository.findByTramiteTramiteId(tramiteId)
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    // Listar comprobantes por tipo (ANTICIPO, DEVOLUCION, REEMBOLSO)
    public List<ComprobanteTransferenciaResponseDTO> listarPorTipo(String tipo) {
        return comprobanteTransferenciaRepository.findByTipo(tipo)
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    // Guardar un nuevo comprobante
    public ComprobanteTransferenciaResponseDTO guardar(ComprobanteTransferenciaRequestDTO dto) {
        Tramite tramite = tramiteRepository.findById(dto.getTramiteId())
                .orElseThrow(() -> new RuntimeException("Trámite no encontrado"));

        ComprobanteTransferencia comprobante = new ComprobanteTransferencia();
        comprobante.setTramite(tramite);
        comprobante.setTipo(dto.getTipo());
        comprobante.setArchivoPdf(dto.getArchivoPdf());
        comprobante.setMonto(dto.getMonto());
        comprobante.setFecha(dto.getFecha() != null ? dto.getFecha() : LocalDate.now());

        ComprobanteTransferencia guardado = comprobanteTransferenciaRepository.save(comprobante);
        return convertirAResponse(guardado);
    }

    // Eliminar un comprobante por ID
    public void eliminar(Long id) {
        comprobanteTransferenciaRepository.deleteById(id);
    }

    // Convertir entidad a DTO de respuesta
    private ComprobanteTransferenciaResponseDTO convertirAResponse(ComprobanteTransferencia comprobante) {
        ComprobanteTransferenciaResponseDTO dto = new ComprobanteTransferenciaResponseDTO();
        dto.setComprobanteId(comprobante.getComprobanteId());
        dto.setTramiteId(comprobante.getTramite().getTramiteId());
        dto.setTipo(comprobante.getTipo());
        dto.setArchivoPdf(comprobante.getArchivoPdf());
        dto.setMonto(comprobante.getMonto());
        dto.setFecha(comprobante.getFecha());
        return dto;
    }
}