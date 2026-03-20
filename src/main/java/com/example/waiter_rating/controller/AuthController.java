package com.example.waiter_rating.controller;

import com.example.waiter_rating.model.*;
import com.example.waiter_rating.repository.*;
import com.example.waiter_rating.service.AppUserService;
import com.example.waiter_rating.service.EmailService;
import com.example.waiter_rating.service.JwtService;
import com.example.waiter_rating.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    private final AppUserRepo appUserRepo;
    private final PasswordEncoder passwordEncoder;
    private final CvRepo cvRepo;
    private final RatingRepo ratingRepo;
    private final QrTokenRepo qrCodeRepo;
    private final JwtService jwtService;
    private final OAuthCodeTokenRepo oAuthCodeTokenRepo;
    private final ProfessionalZoneRepo professionalZoneRepo;
    private final FavoriteProfessionalRepo favoriteProfessionalRepo;
    private final EmailService emailService;
    private final AppUserService appUserService;
    private final RateLimiterService rateLimiter;

    public AuthController(AppUserRepo appUserRepo,
                          PasswordEncoder passwordEncoder,
                          CvRepo cvRepo,
                          RatingRepo ratingRepo,
                          QrTokenRepo qrCodeRepo,
                          JwtService jwtService,
                          OAuthCodeTokenRepo oAuthCodeTokenRepo,
                          ProfessionalZoneRepo professionalZoneRepo,
                          FavoriteProfessionalRepo favoriteProfessionalRepo,
                          EmailService emailService,
                          AppUserService appUserService,
                          RateLimiterService rateLimiter) {
        this.appUserRepo = appUserRepo;
        this.passwordEncoder = passwordEncoder;
        this.cvRepo = cvRepo;
        this.ratingRepo = ratingRepo;
        this.qrCodeRepo = qrCodeRepo;
        this.jwtService = jwtService;
        this.oAuthCodeTokenRepo = oAuthCodeTokenRepo;
        this.professionalZoneRepo = professionalZoneRepo;
        this.favoriteProfessionalRepo = favoriteProfessionalRepo;
        this.emailService = emailService;
        this.appUserService = appUserService;
        this.rateLimiter = rateLimiter;
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : request.getRemoteAddr();
    }

    // ========== HELPERS PRIVADOS ==========

    private String safe(String value) {
        return value != null ? value : "";
    }

    private String validatePassword(String password) {
        if (password.length() < 8) return "La contraseña debe tener al menos 8 caracteres";
        if (!password.matches(".*[a-zA-Z].*")) return "La contraseña debe contener al menos una letra";
        if (!password.matches(".*[0-9].*")) return "La contraseña debe contener al menos un número";
        return null;
    }

    private Map<String, Object> buildClientResponse(AppUser user) {
        return Map.of(
                "id", user.getId(),
                "email", safe(user.getEmail()),
                "name", safe(user.getName()),
                "phone", safe(user.getPhone()),
                "location", safe(user.getLocation()),
                "profilePicture", safe(user.getProfilePicture()),
                "termsAccepted", user.getTermsAccepted() != null && user.getTermsAccepted()
        );
    }

    private Map<String, Object> buildProfessionalResponse(AppUser user) {
        double reputationScore = user.getReputationScore() != null ? user.getReputationScore() : 0.0;
        int totalRatings = user.getTotalRatings() != null ? user.getTotalRatings() : 0;

        return Map.ofEntries(
                Map.entry("id", user.getId()),
                Map.entry("email", safe(user.getEmail())),
                Map.entry("name", safe(user.getName())),
                Map.entry("phone", safe(user.getPhone())),
                Map.entry("location", safe(user.getLocation())),
                Map.entry("professionalTitle", safe(user.getProfessionalTitle())),
                Map.entry("professionType", user.getProfessionType() != null ? user.getProfessionType().name() : ""),
                Map.entry("profilePicture", safe(user.getProfilePicture())),
                Map.entry("reputationScore", reputationScore),
                Map.entry("totalRatings", totalRatings),
                Map.entry("termsAccepted", user.getTermsAccepted() != null && user.getTermsAccepted())
        );
    }

    // ========== OAUTH ==========

    @PostMapping("/exchange-code")
    public ResponseEntity<?> exchangeCode(@RequestBody Map<String, String> request) {
        String code = request.get("code");

        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Código requerido"));
        }

        Optional<OAuthCodeToken> codeTokenOpt = oAuthCodeTokenRepo.findByCodeAndUsedFalse(code);

        if (codeTokenOpt.isEmpty()) {
            log.warn("Intento de intercambio con código inválido o ya usado: {}", code);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Código inválido o ya utilizado"));
        }

        OAuthCodeToken codeToken = codeTokenOpt.get();

        if (codeToken.isExpired()) {
            log.warn("Intento de intercambio con código expirado: {}", code);
            codeToken.setUsed(true);
            oAuthCodeTokenRepo.save(codeToken);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Código expirado"));
        }

        codeToken.setUsed(true);
        oAuthCodeTokenRepo.save(codeToken);

        AppUser user = codeToken.getUser();
        UserRole role = codeToken.getRole();

        String token = jwtService.generateToken(
                user.getId(), role.name(), user.getEmail(), user.getName()
        );

        log.info("Código OAuth intercambiado exitosamente para: {}", user.getEmail());

        try {
            oAuthCodeTokenRepo.deleteExpiredOrUsed(LocalDateTime.now());
        } catch (Exception e) {
            log.debug("Limpieza de códigos expirados omitida: {}", e.getMessage());
        }

        Map<String, Object> userData = role == UserRole.PROFESSIONAL
                ? buildProfessionalResponse(user)
                : buildClientResponse(user);

        return ResponseEntity.ok(Map.of("token", token, "userType", role.name(),
                "id", user.getId(), "email", user.getEmail(), "name", user.getName(),
                "data", userData));
    }

    // ========== REGISTRO ==========

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        if (!rateLimiter.tryConsumeRegister(getClientIp(httpRequest)))
            return ResponseEntity.status(429).body(Map.of("error", "Demasiados intentos. Esperá unos minutos."));

        String email = request.get("email");
        String password = request.get("password");
        String name = request.get("name");
        String location = request.get("location");
        String professionType = request.get("professionType");
        String professionalTitle = request.get("professionalTitle");

        if (email == null || password == null || name == null || professionType == null || professionType.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email, password, nombre y tipo de profesión son requeridos"));
        }

        String passwordError = validatePassword(password);
        if (passwordError != null) return ResponseEntity.badRequest().body(Map.of("error", passwordError));

        if (appUserRepo.findByEmail(email).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "El email ya está registrado"));
        }

        ProfessionType profession;
        try {
            profession = ProfessionType.valueOf(professionType);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Tipo de profesión inválido: " + professionType));
        }

        AppUser professional = AppUser.builder()
                .name(name).email(email)
                .password(passwordEncoder.encode(password))
                .activeRole(UserRole.PROFESSIONAL)
                .location(location).professionType(profession)
                .professionalTitle(professionalTitle)
                .reputationScore(0.0).totalRatings(0).searchable(true)
                .build();

        professional = appUserRepo.save(professional);
        emailService.sendWelcomeEmail(professional.getEmail(), professional.getName());
        appUserService.createVerificationToken(professional);

        String token = jwtService.generateToken(
                professional.getId(), "PROFESSIONAL", professional.getEmail(), professional.getName()
        );

        return ResponseEntity.ok(Map.of(
                "token", token,
                "userType", "PROFESSIONAL",
                "id", professional.getId(),
                "email", professional.getEmail(),
                "name", professional.getName(),
                "reputationScore", 0.0,
                "totalRatings", 0
        ));
    }

    @PostMapping("/register-client")
    public ResponseEntity<?> registerClient(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        if (!rateLimiter.tryConsumeRegister(getClientIp(httpRequest)))
            return ResponseEntity.status(429).body(Map.of("error", "Demasiados intentos. Esperá unos minutos."));

        String email = request.get("email");
        String password = request.get("password");
        String name = request.get("name");

        if (email == null || password == null || name == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email, password y nombre son requeridos"));
        }

        String passwordError = validatePassword(password);
        if (passwordError != null) return ResponseEntity.badRequest().body(Map.of("error", passwordError));

        if (appUserRepo.findByEmail(email).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "El email ya está registrado"));
        }

        AppUser client = AppUser.builder()
                .name(name).email(email)
                .password(passwordEncoder.encode(password))
                .activeRole(UserRole.CLIENT)
                .build();

        client = appUserRepo.save(client);
        emailService.sendWelcomeEmail(client.getEmail(), client.getName());
        appUserService.createVerificationToken(client);

        String token = jwtService.generateToken(
                client.getId(), "CLIENT", client.getEmail(), client.getName()
        );

        return ResponseEntity.ok(Map.of(
                "token", token,
                "userType", "CLIENT",
                "id", client.getId(),
                "email", client.getEmail(),
                "name", client.getName()
        ));
    }

    // ========== LOGIN ==========

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        if (!rateLimiter.tryConsumeLogin(getClientIp(httpRequest)))
            return ResponseEntity.status(429).body(Map.of("error", "Demasiados intentos. Esperá un minuto."));

        String email = request.get("email");
        String password = request.get("password");

        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email y password son requeridos"));
        }

        Optional<AppUser> userOpt = appUserRepo.findByEmail(email);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Credenciales inválidas"));
        }

        AppUser user = userOpt.get();

        if (!UserRole.PROFESSIONAL.equals(user.getActiveRole()) && !UserRole.ADMIN.equals(user.getActiveRole())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Credenciales inválidas"));
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Credenciales inválidas"));
        }

        String token = jwtService.generateToken(
                user.getId(), user.getActiveRole().name(), user.getEmail(), user.getName()
        );

        if (UserRole.ADMIN.equals(user.getActiveRole())) {
            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "userType", "ADMIN",
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "name", user.getName()
            ));
        }

        var response = new java.util.HashMap<>(buildProfessionalResponse(user));
        response.put("token", token);
        response.put("userType", "PROFESSIONAL");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login-client")
    public ResponseEntity<?> loginClient(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        if (!rateLimiter.tryConsumeLogin(getClientIp(httpRequest)))
            return ResponseEntity.status(429).body(Map.of("error", "Demasiados intentos. Esperá un minuto."));

        String email = request.get("email");
        String password = request.get("password");

        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email y password son requeridos"));
        }

        Optional<AppUser> userOpt = appUserRepo.findByEmail(email);

        if (userOpt.isEmpty() || !UserRole.CLIENT.equals(userOpt.get().getActiveRole())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Credenciales inválidas"));
        }

        AppUser client = userOpt.get();

        if (!passwordEncoder.matches(password, client.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Credenciales inválidas"));
        }

        String token = jwtService.generateToken(
                client.getId(), "CLIENT", client.getEmail(), client.getName()
        );

        var response = new java.util.HashMap<>(buildClientResponse(client));
        response.put("token", token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("message", "Logout exitoso"));
    }

    // ========== FORGOT / RESET PASSWORD ==========

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body, HttpServletRequest httpRequest) {
        if (!rateLimiter.tryConsumeForgotPassword(getClientIp(httpRequest)))
            return ResponseEntity.status(429).body(Map.of("error", "Demasiados intentos. Esperá 15 minutos."));

        String email = body.get("email");
        if (email == null || email.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Email requerido"));

        // Siempre responder OK para no revelar si el email existe
        appUserService.requestPasswordReset(email);
        return ResponseEntity.ok(Map.of("message", "Si el email está registrado, recibirás un enlace para restablecer tu contraseña."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String token    = body.get("token");
        String password = body.get("password");

        if (token == null || password == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Token y contraseña son requeridos"));

        String passwordError = validatePassword(password);
        if (passwordError != null)
            return ResponseEntity.badRequest().body(Map.of("error", passwordError));

        boolean ok = appUserService.resetPassword(token, password);
        if (!ok)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "El enlace es inválido o ya expiró"));

        return ResponseEntity.ok(Map.of("message", "Contraseña restablecida correctamente"));
    }

    // ========== EMAIL VERIFICATION ==========

    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        boolean ok = appUserService.verifyEmail(token);
        if (!ok)
            return ResponseEntity.badRequest().body(Map.of("error", "El enlace es inválido o ya expiró"));

        return ResponseEntity.ok(Map.of("message", "Email verificado correctamente"));
    }

    // ========== PERFIL ACTUAL ==========

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String userType = (String) request.getAttribute("userType");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No autenticado"));
        }

        if (!"PROFESSIONAL".equals(userType)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Solo profesionales pueden acceder a este endpoint"));
        }

        Optional<AppUser> userOpt = appUserRepo.findById(userId);
        if (userOpt.isEmpty() || !UserRole.PROFESSIONAL.equals(userOpt.get().getActiveRole())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Profesional no encontrado"));
        }

        return ResponseEntity.ok(buildProfessionalResponse(userOpt.get()));
    }

    @GetMapping("/me/client")
    public ResponseEntity<?> getCurrentClient(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String userType = (String) request.getAttribute("userType");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No autenticado"));
        }

        if (!"CLIENT".equals(userType)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Este endpoint es solo para clientes"));
        }

        Optional<AppUser> userOpt = appUserRepo.findById(userId);
        if (userOpt.isEmpty() || !UserRole.CLIENT.equals(userOpt.get().getActiveRole())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Cliente no encontrado"));
        }

        return ResponseEntity.ok(buildClientResponse(userOpt.get()));
    }

    // ========== ACTUALIZAR PERFIL ==========

    @PutMapping("/update-profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> updates,
                                           HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String userType = (String) request.getAttribute("userType");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No autenticado"));
        }

        Optional<AppUser> userOpt = appUserRepo.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Usuario no encontrado"));
        }

        AppUser user = userOpt.get();

        if (updates.containsKey("phone")) user.setPhone(updates.get("phone"));
        if (updates.containsKey("location")) user.setLocation(updates.get("location"));

        if ("PROFESSIONAL".equals(userType)) {
            if (updates.containsKey("professionalTitle")) {
                user.setProfessionalTitle(updates.get("professionalTitle"));
            }
            if (updates.containsKey("professionType")) {
                try {
                    user.setProfessionType(ProfessionType.valueOf(updates.get("professionType")));
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Tipo de profesión inválido"));
                }
            }
        }

        appUserRepo.save(user);

        return ResponseEntity.ok("PROFESSIONAL".equals(userType)
                ? buildProfessionalResponse(user)
                : buildClientResponse(user));
    }

    // ========== ELIMINAR CUENTA ==========

    @Transactional
    @DeleteMapping("/delete-account/{userId}")
    public ResponseEntity<?> deleteAccount(@PathVariable Long userId, HttpServletRequest request) {
        Long authenticatedUserId = (Long) request.getAttribute("userId");

        if (authenticatedUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No autenticado"));
        }

        if (!authenticatedUserId.equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No tenés permiso para eliminar esta cuenta"));
        }

        try {
            Optional<AppUser> userOpt = appUserRepo.findById(authenticatedUserId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Usuario no encontrado"));
            }

            AppUser user = userOpt.get();

            oAuthCodeTokenRepo.deleteAll(oAuthCodeTokenRepo.findByUserId(authenticatedUserId));

            if (UserRole.PROFESSIONAL.equals(user.getActiveRole())) {
                if (user.getCv() != null) {
                    Cv cv = cvRepo.findById(user.getCv().getId()).orElse(null);
                    if (cv != null) {
                        cv.getZones().clear();
                        cvRepo.save(cv);
                        cvRepo.delete(cv);
                    }
                }

                List<Rating> ratingsRecibidos = ratingRepo.findByProfessionalId(authenticatedUserId);
                ratingsRecibidos.forEach(r -> r.setProfessional(null));
                ratingRepo.saveAll(ratingsRecibidos);

                qrCodeRepo.deleteAll(qrCodeRepo.findByProfessionalId(authenticatedUserId));
                favoriteProfessionalRepo.deleteAll(
                        favoriteProfessionalRepo.findByProfessionalId(authenticatedUserId)
                );
            } else if (UserRole.CLIENT.equals(user.getActiveRole())) {
                log.info(">>> Anonimizando ratings del cliente {}", authenticatedUserId);
                List<Rating> ratingsEmitidas = ratingRepo.findByClientId(authenticatedUserId);
                log.info(">>> Ratings encontradas: {}", ratingsEmitidas.size());
                ratingsEmitidas.forEach(r -> r.setClient(null));
                ratingRepo.saveAll(ratingsEmitidas);
                log.info(">>> Ratings anonimizadas OK");

                favoriteProfessionalRepo.deleteAll(
                        favoriteProfessionalRepo.findByClientIdOrderBySavedAtDesc(authenticatedUserId)
                );
            }

            appUserRepo.deleteById(authenticatedUserId);
            return ResponseEntity.ok(Map.of("message", "Cuenta eliminada exitosamente"));

        } catch (Exception e) {
            log.error("Error eliminando cuenta {}: {}", authenticatedUserId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al eliminar cuenta: " + e.getMessage()));
        }
    }

    // ========== TÉRMINOS Y CONDICIONES ==========

    @PutMapping("/accept-terms")
    public ResponseEntity<?> acceptTerms(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No autenticado"));
        }

        Optional<AppUser> userOpt = appUserRepo.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Usuario no encontrado"));
        }

        AppUser user = userOpt.get();
        user.setTermsAccepted(true);
        user.setTermsAcceptedAt(LocalDateTime.now());
        appUserRepo.save(user);

        log.info("Términos aceptados por user id: {}", userId);
        return ResponseEntity.ok(Map.of("message", "Términos aceptados"));
    }

    @Transactional
    @DeleteMapping("/delete-account-client/{userId}")
    public ResponseEntity<?> deleteClientAccount(@PathVariable Long userId, HttpServletRequest request) {
        Long authenticatedUserId = (Long) request.getAttribute("userId");

        if (authenticatedUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No autenticado"));
        }

        if (!authenticatedUserId.equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No tenés permiso para eliminar esta cuenta"));
        }

        Optional<AppUser> userOpt = appUserRepo.findById(authenticatedUserId);
        if (userOpt.isEmpty() || !UserRole.CLIENT.equals(userOpt.get().getActiveRole())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Cliente no encontrado"));
        }

        List<Rating> ratingsEmitidas = ratingRepo.findByClientId(authenticatedUserId);
        ratingsEmitidas.forEach(r -> r.setClient(null));
        ratingRepo.saveAll(ratingsEmitidas);

        favoriteProfessionalRepo.deleteAll(
                favoriteProfessionalRepo.findByClientIdOrderBySavedAtDesc(authenticatedUserId)
        );

        appUserRepo.deleteById(authenticatedUserId);
        return ResponseEntity.ok(Map.of("message", "Cuenta eliminada exitosamente"));
    }
}