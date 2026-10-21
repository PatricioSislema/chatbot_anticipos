package ec.edu.iti.anticipos.chatbot.controller;

import ec.edu.iti.anticipos.chatbot.dto.response.OrdenResponseDTO;
import ec.edu.iti.anticipos.chatbot.entity.OrdenTransferencia;
import ec.edu.iti.anticipos.chatbot.repository.OrdenTransferenciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ordenes")
@RequiredArgsConstructor
public class OrdenTransferenciaController {

    private final OrdenTransferenciaRepository ordenRepository;

    @GetMapping
    public ResponseEntity<List<OrdenResponseDTO>> listarTodas() {
        List<OrdenTransferencia> ordenes = ordenRepository.findAll(); // Asegúrate de tener el JOIN FETCH aquí

        List<OrdenResponseDTO> response = ordenes.stream().map(o -> {
            OrdenResponseDTO dto = new OrdenResponseDTO();
            dto.setOrdenId(o.getOrdenId());
            dto.setNumeroCuenta(o.getNumeroCuenta());
            dto.setValor(o.getValor());
            dto.setConcepto(o.getConcepto());
            dto.setFechaGeneracion(o.getFechaGeneracion());
            dto.setFechaAprobacion(o.getFechaAprobacion());
            dto.setEstado(o.getEstado());

            if (o.getTramite() != null) {
                dto.setTramiteId(o.getTramite().getTramiteId());
                // NUEVO: Traemos el nombre del funcionario que hizo el trámite
                if (o.getTramite().getUsuario() != null) {
                    dto.setNombreFuncionario(o.getTramite().getUsuario().getNombre());
                }
            }
            if (o.getColectora() != null) dto.setColectoraId(o.getColectora().getUsuarioId());
            if (o.getDirector() != null) dto.setDirectorId(o.getDirector().getUsuarioId());

            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}