package ec.edu.iti.anticipos.chatbot.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Utilidad para generar y validar tokens JWT.
 * Usa el secreto y la expiración configurados en application.properties.
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // Genera el token con el correo, el rol y el ID del usuario
    public String generarToken(String correo, String rol, Long usuarioId) {
        return Jwts.builder()
                .subject(correo)
                .claim("rol", rol)
                .claim("usuarioId", usuarioId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey())
                .compact();
    }

    // Extrae los claims (datos) del token
    public Claims extraerClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Verifica si el token es válido (firma correcta y no expirado)
    public boolean esValido(String token) {
        try {
            extraerClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Extrae el correo del token
    public String extraerCorreo(String token) {
        return extraerClaims(token).getSubject();
    }

    // Extrae el rol del token
    public String extraerRol(String token) {
        return extraerClaims(token).get("rol", String.class);
    }

    // Extrae el ID del usuario del token
    public Long extraerUsuarioId(String token) {
        return extraerClaims(token).get("usuarioId", Long.class);
    }
}
