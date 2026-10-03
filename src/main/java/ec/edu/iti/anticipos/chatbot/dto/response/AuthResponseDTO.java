package ec.edu.iti.anticipos.chatbot.dto.response;

/**
 * DTO de respuesta para el login desde el dashboard web.
 * Devuelve el token JWT junto con datos básicos del usuario.
 */
public class AuthResponseDTO {

    private String token;
    private String rol;
    private String nombre;
    private String correo;
    private Long usuarioId;

    public AuthResponseDTO(String token, String rol, String nombre, String correo, Long usuarioId) {
        this.token = token;
        this.rol = rol;
        this.nombre = nombre;
        this.correo = correo;
        this.usuarioId = usuarioId;
    }

    // Getters y setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
}
