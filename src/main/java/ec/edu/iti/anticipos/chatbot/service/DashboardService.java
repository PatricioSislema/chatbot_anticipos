package ec.edu.iti.anticipos.chatbot.service;

import ec.edu.iti.anticipos.chatbot.dto.response.DashboardStatsDTO;
import ec.edu.iti.anticipos.chatbot.repository.TramiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Servicio con la lógica de las métricas del dashboard.
 * Reutiliza el repositorio de Tramite para contar por estado.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TramiteRepository tramiteRepository;

    public DashboardStatsDTO obtenerEstadisticas() {
        DashboardStatsDTO dto = new DashboardStatsDTO();

        dto.setPendientes(tramiteRepository.countByEstado("Pendiente"));
        dto.setAprobados(tramiteRepository.countByEstado("Aprobado por Contadora")
                + tramiteRepository.countByEstado("Aprobado por Jefe")
                + tramiteRepository.countByEstado("Aprobado por Director"));
        dto.setRechazados(tramiteRepository.countByEstado("Rechazado"));
        dto.setTransferidos(tramiteRepository.countByEstado("Transferido"));
        dto.setLiquidacionesEnProceso(tramiteRepository.countByEstado("Liquidación en proceso"));
        dto.setLiquidacionesAprobadas(tramiteRepository.countByEstado("Liquidación aprobada"));
        dto.setLiquidacionesCerradas(tramiteRepository.countByEstado("Liquidación cerrada"));
        dto.setTotalTramites(tramiteRepository.count());
        dto.setMontoTotal(BigDecimal.ZERO); // Se conectará después

        return dto;
    }
}
