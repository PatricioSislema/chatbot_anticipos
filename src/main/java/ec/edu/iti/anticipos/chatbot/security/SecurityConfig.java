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

                        // ===== ENDPOINTS CON ROL ESTRICTO =====
                        // Usamos hasAuthority para que coincida con el nombre exacto de la BD ("Administrador")
                        .requestMatchers("/api/usuarios/**").hasAuthority("Administrador")
                        .requestMatchers("/api/roles/**").hasAuthority("Administrador")
                        .requestMatchers("/api/tipos-tramite/**").hasAuthority("Administrador")

                        // ===== ENDPOINTS PARA TODOS LOS AUTENTICADOS =====
                        .requestMatchers("/api/ordenes/**").authenticated()
                        .requestMatchers("/api/reembolsos/**").authenticated()
                        .requestMatchers("/api/tramites/**").authenticated()

                        // ===== TODOS LOS DEMÁS REQUIEREN JWT =====
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
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