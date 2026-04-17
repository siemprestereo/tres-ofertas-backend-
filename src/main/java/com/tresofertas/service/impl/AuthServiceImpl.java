package com.tresofertas.service.impl;

import com.tresofertas.dto.request.ForgotPasswordRequest;
import com.tresofertas.dto.request.LoginRequest;
import com.tresofertas.dto.request.RegisterRequest;
import com.tresofertas.dto.request.ResetPasswordRequest;
import com.tresofertas.dto.response.AuthResponse;
import com.tresofertas.model.AppUser;
import com.tresofertas.model.PasswordResetToken;
import com.tresofertas.repository.AppUserRepo;
import com.tresofertas.repository.PasswordResetTokenRepo;
import com.tresofertas.security.JwtService;
import com.tresofertas.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final AppUserRepo userRepo;
    private final PasswordResetTokenRepo resetTokenRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(AppUserRepo userRepo, PasswordResetTokenRepo resetTokenRepo,
                           PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepo = userRepo;
        this.resetTokenRepo = resetTokenRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepo.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El email ya está registrado");
        }

        AppUser.Role role;
        try {
            role = AppUser.Role.valueOf(request.getRole().toUpperCase());
        } catch (Exception e) {
            throw new RuntimeException("Rol inválido. Use MERCHANT o CONSUMER");
        }

        if (role == AppUser.Role.ADMIN) {
            throw new RuntimeException("No se puede registrar como ADMIN");
        }

        AppUser user = new AppUser();
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        userRepo.save(user);

        String token = jwtService.generateToken(user.getId(), user.getRole().name(), user.getEmail());
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getName(), user.getRole().name());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        AppUser user = userRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Credenciales inválidas"));

        if (!user.getActive()) {
            throw new RuntimeException("Cuenta desactivada");
        }

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Credenciales inválidas");
        }

        String token = jwtService.generateToken(user.getId(), user.getRole().name(), user.getEmail());
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getName(), user.getRole().name());
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepo.findByEmail(request.getEmail()).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            PasswordResetToken prt = new PasswordResetToken();
            prt.setUser(user);
            prt.setToken(token);
            prt.setExpiresAt(LocalDateTime.now().plusHours(1));
            resetTokenRepo.save(prt);
            // TODO: enviar email con el token
        });
        // Responde igual exista o no el email (anti-enumeración)
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken prt = resetTokenRepo.findByToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Token inválido"));

        if (prt.getUsed()) throw new RuntimeException("Token ya utilizado");
        if (prt.getExpiresAt().isBefore(LocalDateTime.now())) throw new RuntimeException("Token expirado");

        AppUser user = prt.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepo.save(user);

        prt.setUsed(true);
        resetTokenRepo.save(prt);
    }
}
