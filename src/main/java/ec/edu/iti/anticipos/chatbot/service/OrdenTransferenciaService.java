package ec.edu.iti.anticipos.chatbot.service;

import ec.edu.iti.anticipos.chatbot.dto.request.OrdenTransferenciaRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.OrdenTransferenciaResponseDTO;
import ec.edu.iti.anticipos.chatbot.entity.OrdenTransferencia;
import ec.edu.iti.anticipos.chatbot.entity.Tramite;
import ec.edu.iti.anticipos.chatbot.entity.Usuario;
import ec.edu.iti.anticipos.chatbot.repository.OrdenTransferenciaRepository;
import ec.edu.iti.anticipos.chatbot.repository.TramiteRepository;
import ec.edu.iti.anticipos.chatbot.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Servicio para la entidad OrdenTransferencia.
 * Contiene la lógica de negocio relacionada con las órdenes de transferencia
 * que la Colectora genera y el Director Financiero aprueba.
 */
@Service
@RequiredArgsConstructor
public class OrdenTransferenciaService {

    private final OrdenTransferenciaRepository ordenTransferenciaRepository;
    private final TramiteRepository tramiteRepository;
    private final UsuarioRepository usuarioRepository;

    // Listar todas las órdenes
    public List<OrdenTransferenciaResponseDTO> listarTodos() {
        return ordenTransferenciaRepository.findAll()
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    // Buscar una orden por ID
    public Optional<OrdenTransferenciaResponseDTO> buscarPorId(Long id) {
        return ordenTransferenciaRepository.findById(id)
                .map(this::convertirAResponse);
    }

    // Buscar la orden de un trámite específico (CORREGIDO AQUÍ)
    public Optional<OrdenTransferenciaResponseDTO> buscarPorTramite(Long tramiteId) {
        return ordenTransferenciaRepository.findByTramiteTramiteId(tramiteId)
                .stream()
                .findFirst()
                .map(this::convertirAResponse);
    }

    // Listar órdenes por estado
    public List<OrdenTransferenciaResponseDTO> listarPorEstado(String estado) {
        return ordenTransferenciaRepository.findByEstado(estado)
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    // Guardar una nueva orden
    public OrdenTransferenciaResponseDTO guardar(OrdenTransferenciaRequestDTO dto) {
        Tramite tramite = tramiteRepository.findById(dto.getTramiteId())
                .orElseThrow(() -> new RuntimeException("Trámite no encontrado"));

        Usuario colectora = usuarioRepository.findById(dto.getColectoraId())
                .orElseThrow(() -> new RuntimeException("Colectora no encontrada"));

        OrdenTransferencia orden = new OrdenTransferencia();
        orden.setTramite(tramite);
        orden.setColectora(colectora);
        orden.setNumeroCuenta(dto.getNumeroCuenta());
        orden.setValor(dto.getValor());
        orden.setConcepto(dto.getConcepto());
        orden.setFechaGeneracion(dto.getFechaGeneracion() != null ? dto.getFechaGeneracion() : LocalDate.now());
        orden.setFechaAprobacion(dto.getFechaAprobacion());
        orden.setEstado(dto.getEstado() != null ? dto.getEstado() : "Generada");

        if (dto.getDirectorId() != null) {
            Usuario director = usuarioRepository.findById(dto.getDirectorId())
                    .orElseThrow(() -> new RuntimeException("Director no encontrado"));
            orden.setDirector(director);
        }

        OrdenTransferencia guardada = ordenTransferenciaRepository.save(orden);
        return convertirAResponse(guardada);
    }

    // Actualizar una orden existente
    public OrdenTransferenciaResponseDTO actualizar(Long id, OrdenTransferenciaRequestDTO dto) {
        OrdenTransferencia orden = ordenTransferenciaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));

        orden.setNumeroCuenta(dto.getNumeroCuenta());
        orden.setValor(dto.getValor());
        orden.setConcepto(dto.getConcepto());
        orden.setFechaAprobacion(dto.getFechaAprobacion());
        orden.setEstado(dto.getEstado());

        if (dto.getDirectorId() != null) {
            Usuario director = usuarioRepository.findById(dto.getDirectorId())
                    .orElseThrow(() -> new RuntimeException("Director no encontrado"));
            orden.setDirector(director);
        }

        OrdenTransferencia actualizada = ordenTransferenciaRepository.save(orden);
        return convertirAResponse(actualizada);
    }

    // Eliminar una orden por ID
    public void eliminar(Long id) {
        ordenTransferenciaRepository.deleteById(id);
    }

    // Convertir entidad a DTO de respuesta
    private OrdenTransferenciaResponseDTO convertirAResponse(OrdenTransferencia orden) {
        OrdenTransferenciaResponseDTO dto = new OrdenTransferenciaResponseDTO();
        dto.setOrdenId(orden.getOrdenId());
        dto.setTramiteId(orden.getTramite().getTramiteId());
        dto.setColectoraId(orden.getColectora().getUsuarioId());
        dto.setColectoraNombre(orden.getColectora().getNombre());
        dto.setNumeroCuenta(orden.getNumeroCuenta());
        dto.setValor(orden.getValor());
        dto.setConcepto(orden.getConcepto());
        dto.setFechaGeneracion(orden.getFechaGeneracion());
        dto.setFechaAprobacion(orden.getFechaAprobacion());
        dto.setEstado(orden.getEstado());

        if (orden.getDirector() != null) {
            dto.setDirectorId(orden.getDirector().getUsuarioId());
            dto.setDirectorNombre(orden.getDirector().getNombre());
        }

        return dto;
    }
}