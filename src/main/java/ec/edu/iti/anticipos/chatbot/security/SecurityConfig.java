package ec.edu.iti.anticipos.chatbot.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración de Spring Security.
 * - Habilita CORS para el frontend (React en localhost:5173)
 * - Define qué endpoints son públicos y cuáles requieren JWT
 * - Registra el filtro JWT
 * - Define el PasswordEncoder (BCrypt)
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ===== ENDPOINTS PÚBLICOS =====
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/usuarios/login").permitAll()
                        .requestMatchers("/api/telegram/**").permitAll()
                        .requestMatchers("/api/chatbot/**").permitAll()

                        // ===== ENDPOINTS SOLO ADMINISTRADOR =====
                        .requestMatchers("/api/roles/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/tipos-tramite/**").hasRole("ADMINISTRADOR")

                        // ===== TODOS LOS DEMÁS REQUIEREN JWT =====
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",           // React con Vite
                "http://localhost:3000",           // React con CRA (por si acaso)
                "https://tu-app.azurestaticapps.net" // cuando despliegues
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}