package ec.edu.iti.anticipos.chatbot.dto.response;

import java.math.BigDecimal;

/**
 * DTO con las métricas del dashboard.
 * Los estados se mapean tal como están en la BD (strings libres).
 */
public class DashboardStatsDTO {

    private long pendientes;
    private long aprobados;
    private long rechazados;
    private long transferidos;
    private long liquidacionesEnProceso;
    private long liquidacionesAprobadas;
    private long liquidacionesCerradas;
    private long totalTramites;
    private BigDecimal montoTotal;

    // Getters y setters
    public long getPendientes() { return pendientes; }
    public void setPendientes(long pendientes) { this.pendientes = pendientes; }

    public long getAprobados() { return aprobados; }
    public void setAprobados(long aprobados) { this.aprobados = aprobados; }

    public long getRechazados() { return rechazados; }
    public void setRechazados(long rechazados) { this.rechazados = rechazados; }

    public long getTransferidos() { return transferidos; }
    public void setTransferidos(long transferidos) { this.transferidos = transferidos; }

    public long getLiquidacionesEnProceso() { return liquidacionesEnProceso; }
    public void setLiquidacionesEnProceso(long liquidacionesEnProceso) { this.liquidacionesEnProceso = liquidacionesEnProceso; }

    public long getLiquidacionesAprobadas() { return liquidacionesAprobadas; }
    public void setLiquidacionesAprobadas(long liquidacionesAprobadas) { this.liquidacionesAprobadas = liquidacionesAprobadas; }

    public long getLiquidacionesCerradas() { return liquidacionesCerradas; }
    public void setLiquidacionesCerradas(long liquidacionesCerradas) { this.liquidacionesCerradas = liquidacionesCerradas; }

    public long getTotalTramites() { return totalTramites; }
    public void setTotalTramites(long totalTramites) { this.totalTramites = totalTramites; }

    public BigDecimal getMontoTotal() { return montoTotal; }
    public void setMontoTotal(BigDecimal montoTotal) { this.montoTotal = montoTotal; }
}