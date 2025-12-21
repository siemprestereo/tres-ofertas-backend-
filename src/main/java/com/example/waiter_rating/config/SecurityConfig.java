package com.example.waiter_rating.config;

import com.example.waiter_rating.security.OAuth2LoginSuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    public SecurityConfig(OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler) {
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // ========== PÚBLICOS (sin autenticación) ==========
                        .requestMatchers(
                                "/",
                                "/login",
                                "/oauth2/**",
                                "/login/oauth2/**"
                        ).permitAll()

                        // Auth endpoints para meseros (registro/login)
                        .requestMatchers("/api/auth/**").permitAll()

                        // Perfiles públicos de meseros (para empleadores)
                        .requestMatchers(HttpMethod.GET, "/api/waiters/*/public").permitAll()

                        // QRs públicos (para escanear y resolver)
                        .requestMatchers("/qr/**", "/api/qr/resolve/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/qr/{code}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/qr/resolve/{code}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/qr/dynamic").permitAll()

                        // Ver ratings públicos de un mesero
                        .requestMatchers(HttpMethod.GET, "/api/ratings/professional/**").permitAll()

                        // Ver CV público
                        .requestMatchers(HttpMethod.GET, "/api/cv/professional/**").permitAll()

                        // Listar restaurantes (público)
                        .requestMatchers(HttpMethod.GET, "/api/restaurants", "/api/restaurants/*").permitAll()

                        // ========== SOLO PARA TESTING (eliminar en producción) ==========
                        .requestMatchers(HttpMethod.POST, "/api/clients", "/api/waiters").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/cv/professional/**").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/cv/professional/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/restaurants").permitAll()
                        // ❌ REMOVIDO: .requestMatchers(HttpMethod.POST, "/api/qr/generate").permitAll()
                        .requestMatchers("/api/cv/me/**").permitAll()

                        // ========== ENDPOINTS PROTEGIDOS (requieren autenticación) ==========
                        // QR - Generar (solo profesionales autenticados)
                        .requestMatchers(HttpMethod.POST, "/api/qr/generate").authenticated()  // ← AGREGADO

                        // QR - Invalidar (solo meseros autenticados)
                        .requestMatchers(HttpMethod.POST, "/api/qr/{code}/invalidate").authenticated()

                        // Stats (solo meseros)
                        .requestMatchers("/api/stats/**").authenticated()

                        // Cualquier otro endpoint requiere autenticación
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.ALWAYS)  // ← CAMBIO: IF_REQUIRED → ALWAYS
                )
                .securityContext(context -> context
                        .requireExplicitSave(false)  // ← AGREGADO: Auto-guardar el contexto en la sesión
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2LoginSuccessHandler)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Usar variable de entorno para allowed origins
        configuration.setAllowedOriginPatterns(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("Set-Cookie"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}