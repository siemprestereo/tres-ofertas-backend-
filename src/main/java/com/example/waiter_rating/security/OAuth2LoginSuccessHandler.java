package com.example.waiter_rating.security;

import com.example.waiter_rating.model.Client;
import com.example.waiter_rating.model.Professional;
import com.example.waiter_rating.service.ClientService;
import com.example.waiter_rating.service.ProfessionalService;
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
    private final ProfessionalService professionalService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public OAuth2LoginSuccessHandler(ClientService clientService, ProfessionalService professionalService) {
        this.clientService = clientService;
        this.professionalService = professionalService;
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

        // Verificar si es un Professional
        Professional professional = professionalService.findByEmail(email);

        if (professional != null) {
            // Es un Professional
            System.out.println("✅ Profesional autenticado: " + professional.getName());

            HttpSession session = request.getSession(true);
            session.setAttribute("userId", professional.getId());
            session.setAttribute("userType", "PROFESSIONAL");

            // Verificar si tiene perfil completo
            boolean hasCompleteProfile = professional.getCv() != null;

            String redirectUrl;
            if (hasCompleteProfile) {
                redirectUrl = frontendUrl + "/professional-dashboard";
            } else {
                redirectUrl = frontendUrl + "/professional-register?step=complete-profile";
            }

            System.out.println("🔄 Redirigiendo profesional a: " + redirectUrl);
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
            return;
        }

        // Si no es Professional, crear como Client
        Client client = clientService.findOrCreateFromGoogle(email, name, googleId, emailVerified);

        if (client.getId() != null) {
            System.out.println("✅ Cliente autenticado: " + client.getName() + " (ID: " + client.getId() + ")");

            HttpSession session = request.getSession(true);
            session.setAttribute("userId", client.getId());
            session.setAttribute("userType", "CLIENT");

            System.out.println("✅ Sesión HTTP creada: " + session.getId());
        }

        // Redirigir cliente a su dashboard
        String redirectUrl = frontendUrl + "/client-dashboard";
        System.out.println("🔄 Redirigiendo cliente a: " + redirectUrl);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}