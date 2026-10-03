package ec.edu.iti.anticipos.chatbot.dto.response;

import java.time.LocalDate;

/**
 * DTO resumido para la tabla de trámites del dashboard.
 * Contiene solo los campos necesarios para mostrar en la lista.
 */
public class TramiteResumenDTO {

    private Long tramiteId;
    private String estado;
    private LocalDate fechaCreacion;
    private String nombreUsuario;
    private String nombreTipoTramite;

    // Getters y setters
    public Long getTramiteId() { return tramiteId; }
    public void setTramiteId(Long tramiteId) { this.tramiteId = tramiteId; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public LocalDate getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDate fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public String getNombreTipoTramite() { return nombreTipoTramite; }
    public void setNombreTipoTramite(String nombreTipoTramite) { this.nombreTipoTramite = nombreTipoTramite; }
}