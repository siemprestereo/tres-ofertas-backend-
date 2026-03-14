package com.example.waiter_rating.security;

import com.example.waiter_rating.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7);
            Claims claims = jwtService.validateToken(token);

            Long userId = claims.get("userId", Long.class);
            String userType = claims.get("userType", String.class);
            String email = claims.get("email", String.class);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                String role = "ROLE_" + userType;
                List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(email, null, authorities);

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                request.setAttribute("userId", userId);
                request.setAttribute("userType", userType);

                log.debug("JWT validado para usuario: {} con rol: {}", email, role);
            }

        } catch (JwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
            log.warn("Intento de acceso con JWT inválido: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}