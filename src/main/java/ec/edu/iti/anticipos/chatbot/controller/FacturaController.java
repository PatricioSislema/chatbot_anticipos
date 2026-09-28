package ec.edu.iti.anticipos.chatbot.controller;

import ec.edu.iti.anticipos.chatbot.dto.request.FacturaRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.FacturaResponseDTO;
import ec.edu.iti.anticipos.chatbot.service.FacturaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ec.edu.iti.anticipos.chatbot.dto.response.FacturaExtraidaDTO;
import ec.edu.iti.anticipos.chatbot.service.DocumentIntelligenceService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Controlador REST para la entidad Factura.
 * Expone los endpoints para gestionar las facturas subidas durante la liquidación.
 */
@RestController
@RequestMapping("/api/facturas")
@RequiredArgsConstructor
public class FacturaController {

    private final FacturaService facturaService;
    private final DocumentIntelligenceService documentIntelligenceService;

    // Listar todas las facturas
    @GetMapping
    public ResponseEntity<List<FacturaResponseDTO>> listar() {
        return ResponseEntity.ok(facturaService.listarTodos());
    }

    // Buscar una factura por ID
    @GetMapping("/{id}")
    public ResponseEntity<FacturaResponseDTO> buscar(@PathVariable Long id) {
        return facturaService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Crear una nueva factura
    @PostMapping
    public ResponseEntity<FacturaResponseDTO> crear(@RequestBody FacturaRequestDTO dto) {
        return ResponseEntity.ok(facturaService.guardar(dto));
    }

    // Eliminar una factura por ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        facturaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // Extraer datos de una factura PDF con Azure Document Intelligence
    @PostMapping("/extraer")
    public ResponseEntity<FacturaExtraidaDTO> extraerFactura(@RequestParam("archivo") MultipartFile archivo) {
        try {
            byte[] bytes = archivo.getBytes();
            FacturaExtraidaDTO resultado = documentIntelligenceService.extraerDatos(bytes);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}