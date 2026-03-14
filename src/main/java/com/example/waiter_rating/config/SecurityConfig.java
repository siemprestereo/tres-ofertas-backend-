package com.example.waiter_rating.config;

import com.example.waiter_rating.security.JwtAuthenticationFilter;
import com.example.waiter_rating.security.OAuth2LoginSuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
@EnableMethodSecurity
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

                        .requestMatchers("/api/auth/**").permitAll()

                        .requestMatchers("/qr/**", "/api/qr/resolve/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/qr/{code}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/qr/resolve/{code}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/qr/dynamic").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/ratings/professional/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/ratings/work-history/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/stats/professional/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/cv/professional/**").permitAll()
                        .requestMatchers("/api/cv/*/download-pdf").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/professionals/search").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/professionals/search/top").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/professionals/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/professionals").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/professionals/{id}/can-change-workplace").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/restaurants", "/api/restaurants/*").permitAll()

                        // ========== ADMIN ==========
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // ========== PROTEGIDOS ==========
                        .requestMatchers(HttpMethod.POST, "/api/qr/generate").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/qr/{code}/invalidate").authenticated()
                        .requestMatchers("/api/stats/**").authenticated()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
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

        if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
            configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        }

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept"));
        configuration.setAllowCredentials(true);
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