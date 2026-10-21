package ec.edu.iti.anticipos.chatbot.controller;

import ec.edu.iti.anticipos.chatbot.dto.response.ReembolsoResponseDTO;
import ec.edu.iti.anticipos.chatbot.entity.Reembolso;
import ec.edu.iti.anticipos.chatbot.repository.ReembolsoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reembolsos")
@RequiredArgsConstructor
public class ReembolsoController {

    private final ReembolsoRepository reembolsoRepository;

    @GetMapping
    public ResponseEntity<List<ReembolsoResponseDTO>> listarTodos() {
        List<Reembolso> reembolsos = reembolsoRepository.findAll();

        List<ReembolsoResponseDTO> response = reembolsos.stream().map(r -> {
            ReembolsoResponseDTO dto = new ReembolsoResponseDTO();
            dto.setReembolsoId(r.getReembolsoId());
            dto.setMontoSolicitado(r.getMontoSolicitado());
            dto.setFechaSolicitud(r.getFechaSolicitud());
            dto.setFechaAutorizacion(r.getFechaAutorizacion());
            dto.setEstado(r.getEstado());
            dto.setObservacion(r.getObservacion());

            // Acceso seguro a las relaciones
            if (r.getAutorizadoPor() != null) {
                dto.setAutorizadoPorId(r.getAutorizadoPor().getUsuarioId());
                dto.setAutorizadoPorNombre(r.getAutorizadoPor().getNombre());
            }
            if (r.getLiquidacion() != null) {
                dto.setLiquidacionId(r.getLiquidacion().getLiquidacionId());
                if (r.getLiquidacion().getTramite() != null) {
                    dto.setTramiteId(r.getLiquidacion().getTramite().getTramiteId());
                }
            }

            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}