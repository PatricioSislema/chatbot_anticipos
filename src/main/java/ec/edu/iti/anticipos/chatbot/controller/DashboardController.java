package ec.edu.iti.anticipos.chatbot.controller;

import ec.edu.iti.anticipos.chatbot.dto.response.DashboardStatsDTO;
import ec.edu.iti.anticipos.chatbot.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para las métricas del dashboard.
 * Requiere token JWT válido.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDTO> stats() {
        return ResponseEntity.ok(dashboardService.obtenerEstadisticas());
    }
}