package ec.edu.iti.anticipos.chatbot.controller;

import ec.edu.iti.anticipos.chatbot.dto.request.LiquidacionRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.LiquidacionResponseDTO;
import ec.edu.iti.anticipos.chatbot.service.LiquidacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controlador REST para la entidad Liquidacion.
 * Expone los endpoints para gestionar las liquidaciones de anticipo.
 */
@RestController
@RequestMapping("/api/liquidaciones")
@RequiredArgsConstructor
public class LiquidacionController {

    private final LiquidacionService liquidacionService;

    // Listar todas las liquidaciones
    @GetMapping
    public ResponseEntity<List<LiquidacionResponseDTO>> listar() {
        return ResponseEntity.ok(liquidacionService.listarTodos());
    }

    // Buscar una liquidación por ID
    @GetMapping("/{id}")
    public ResponseEntity<LiquidacionResponseDTO> buscar(@PathVariable Long id) {
        return liquidacionService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Crear una nueva liquidación
    @PostMapping
    public ResponseEntity<LiquidacionResponseDTO> crear(@RequestBody LiquidacionRequestDTO dto) {
        return ResponseEntity.ok(liquidacionService.guardar(dto));
    }

    // Calcular el saldo (anticipo - total gastado)
    @GetMapping("/calcular-saldo")
    public ResponseEntity<BigDecimal> calcularSaldo(
            @RequestParam BigDecimal anticipo,
            @RequestParam BigDecimal totalGastado) {
        return ResponseEntity.ok(liquidacionService.calcularSaldo(anticipo, totalGastado));
    }

    // Eliminar una liquidación por ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        liquidacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
