package ec.edu.iti.anticipos.chatbot.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro que intercepta cada petición HTTP, valida el token JWT
 * y establece el usuario autenticado en el contexto de Spring Security.
 */
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            if (jwtUtil.esValido(token)) {
                String correo = jwtUtil.extraerCorreo(token);
                String rol = jwtUtil.extraerRol(token);
                Long usuarioId = jwtUtil.extraerUsuarioId(token);

                // Creamos la autenticación con el rol en formato ROLE_XXX
                var auth = new UsernamePasswordAuthenticationToken(
                        correo,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + rol.toUpperCase().replace(" ", "_")))
                );

                // Guardamos el usuarioId como detalle para usarlo después
                auth.setDetails(usuarioId);

                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        chain.doFilter(request, response);
    }
}