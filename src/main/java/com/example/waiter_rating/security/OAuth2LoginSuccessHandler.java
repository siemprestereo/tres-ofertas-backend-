package com.example.waiter_rating.security;

import com.example.waiter_rating.model.Client;
import com.example.waiter_rating.service.ClientService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final ClientService clientService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public OAuth2LoginSuccessHandler(ClientService clientService) {
        this.clientService = clientService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String googleId = oAuth2User.getAttribute("sub");
        Boolean emailVerified = oAuth2User.getAttribute("email_verified");

        System.out.println("🔍 LOGIN CON GOOGLE:");
        System.out.println("   Email: " + email);
        System.out.println("   Name: " + name);
        System.out.println("   Google ID: " + googleId);

        Client client = clientService.findOrCreateFromGoogle(email, name, googleId, emailVerified);

        if (client.getId() != null) {
            System.out.println("✅ Cliente autenticado: " + client.getName() + " (ID: " + client.getId() + ")");

            // ✅ Crear sesión HTTP explícitamente
            HttpSession session = request.getSession(true);
            session.setAttribute("userId", client.getId());
            session.setAttribute("userType", "CLIENT");

            System.out.println("✅ Sesión HTTP creada: " + session.getId());
        }

        // ✅ Redirigir al frontend (usa la URL del application.properties)
        System.out.println("🔄 Redirigiendo a: " + frontendUrl);
        getRedirectStrategy().sendRedirect(request, response, frontendUrl);
    }
}