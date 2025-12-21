package com.example.waiter_rating.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, Object> home(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return Map.of(
                    "message", "Bienvenido a Professional Rating",
                    "authenticated", false
            );
        }

        return Map.of(
                "message", "Bienvenido a Professional Rating",
                "authenticated", true,
                "name", principal.getAttribute("name"),
                "email", principal.getAttribute("email")
        );
    }

    @GetMapping("/oauth2/success")
    public Map<String, String> success() {
        return Map.of("message", "Login exitoso con Google!");
    }
}