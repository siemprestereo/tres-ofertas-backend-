package com.example.waiter_rating.config;

import com.example.waiter_rating.security.JwtAuthenticationFilter;
import com.example.waiter_rating.security.OAuth2LoginSuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    public SecurityConfig(OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
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

                        // Ver los ratings públicos de un mesero
                        .requestMatchers(HttpMethod.GET, "/api/ratings/professional/**").permitAll()

                        // Ver ratings públicos por workHistoryId (para CV compartido)
                        .requestMatchers(HttpMethod.GET, "/api/ratings/work-history/**").permitAll()

                        // Ver estadísticas públicas de un profesional
                        .requestMatchers(HttpMethod.GET, "/api/stats/professional/**").permitAll()

                        // Ver CV público
                        .requestMatchers(HttpMethod.GET, "/api/cv/professional/**").permitAll()

                        // Download PDF
                        .requestMatchers("/api/cv/*/download-pdf").permitAll()

                        // Ver perfil público de profesional
                        // Profesionales - Endpoints PÚBLICOS (solo lectura específica)
                        .requestMatchers(HttpMethod.GET, "/api/professionals/search").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/professionals/search/top").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/professionals/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/professionals").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/professionals/{id}/can-change-workplace").permitAll()

                        // Listar restaurantes (público)
                        .requestMatchers(HttpMethod.GET, "/api/restaurants", "/api/restaurants/*").permitAll()

                        // ========== SOLO PARA TESTING (eliminar en producción) ==========


                        // ========== ENDPOINTS PROTEGIDOS (requieren autenticación) ==========
                        // QR - Generar (solo profesionales autenticados)
                        .requestMatchers(HttpMethod.POST, "/api/qr/generate").authenticated()

                        // QR - Invalidar (solo meseros autenticados)
                        .requestMatchers(HttpMethod.POST, "/api/qr/{code}/invalidate").authenticated()

                        // Stats (solo meseros)
                        .requestMatchers("/api/stats/**").authenticated()

                        // Cualquier otro endpoint requiere autenticación
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)  // ← CAMBIO: STATELESS para JWT
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)  // ← AGREGAR filtro JWT
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2LoginSuccessHandler)
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\": \"No autenticado\"}");
                        })
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

        // 1. Cargamos el origen desde la variable que ya tienes en Railway
        if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
            // Usamos setAllowedOrigins (más estricto y seguro para producción)
            configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        }

        // 2. Métodos permitidos para Calificalo
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // 3. Headers permitidos (limitamos a los necesarios)
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept"));

        // 4. Permitir credenciales (fundamental para tu flujo OAuth2 y JWT)
        configuration.setAllowCredentials(true);

        // 5. Exponer headers para que el Frontend (React/Vue) pueda leerlos
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Set-Cookie"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}