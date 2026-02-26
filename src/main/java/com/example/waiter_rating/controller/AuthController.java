package com.example.waiter_rating.controller;

import com.example.waiter_rating.model.*;
import com.example.waiter_rating.repository.*;
import com.example.waiter_rating.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    private static final String UPLOAD_DIR = "uploads/profiles/";

    public AuthController(AppUserRepo appUserRepo,
                          PasswordEncoder passwordEncoder,
                          CvRepo cvRepo,
                          RatingRepo ratingRepo,
                          QrTokenRepo qrCodeRepo,
                          JwtService jwtService,
                          OAuthCodeTokenRepo oAuthCodeTokenRepo) {
        this.appUserRepo = appUserRepo;
        this.passwordEncoder = passwordEncoder;
        this.cvRepo = cvRepo;
        this.ratingRepo = ratingRepo;
        this.qrCodeRepo = qrCodeRepo;
        this.jwtService = jwtService;
        this.oAuthCodeTokenRepo = oAuthCodeTokenRepo;

        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Intercambia un código temporal OAuth por un JWT real.
     * El código es de un solo uso y expira en 60 segundos.
     */
    @PostMapping("/exchange-code")
    public ResponseEntity<?> exchangeCode(@RequestBody Map<String, String> request) {
        String code = request.get("code");

        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Código requerido"));
        }

        Optional<OAuthCodeToken> codeTokenOpt = oAuthCodeTokenRepo.findByCodeAndUsedFalse(code);

        if (codeTokenOpt.isEmpty()) {
            log.warn("Intento de intercambio con código inválido o ya usado: {}", code);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Código inválido o ya utilizado"));
        }

        OAuthCodeToken codeToken = codeTokenOpt.get();

        // Verificar expiración
        if (codeToken.isExpired()) {
            log.warn("Intento de intercambio con código expirado: {}", code);
            codeToken.setUsed(true);
            oAuthCodeTokenRepo.save(codeToken);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Código expirado"));
        }

        // Marcar como usado inmediatamente (un solo uso)
        codeToken.setUsed(true);
        oAuthCodeTokenRepo.save(codeToken);

        // Generar el JWT real
        AppUser user = codeToken.getUser();
        UserRole role = codeToken.getRole();

        String token = jwtService.generateToken(
                user.getId(),
                role.name(),
                user.getEmail(),
                user.getName()
        );

        log.info("Código OAuth intercambiado exitosamente para: {}", user.getEmail());

        // Limpiar códigos expirados/usados (limpieza oportunista)
        try {
            oAuthCodeTokenRepo.deleteExpiredOrUsed(LocalDateTime.now());
        } catch (Exception e) {
            log.debug("Limpieza de códigos expirados omitida: {}", e.getMessage());
        }

        // Devolver token + datos del usuario
        if (role == UserRole.PROFESSIONAL) {
            double reputationScore = user.getReputationScore() != null ? user.getReputationScore() : 0.0;
            int totalRatings = user.getTotalRatings() != null ? user.getTotalRatings() : 0;

            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "name", user.getName(),
                    "userType", "PROFESSIONAL",
                    "reputationScore", reputationScore,
                    "totalRatings", totalRatings
            ));
        } else {
            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "name", user.getName(),
                    "userType", "CLIENT"
            ));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");
        String name = request.get("name");
        String location = request.get("location");
        String professionType = request.get("professionType");
        String professionalTitle = request.get("professionalTitle");

        if (email == null || password == null || name == null || professionType == null || professionType.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email, password, nombre y tipo de profesión son requeridos"));
        }

        String passwordError = validatePassword(password);
        if (passwordError != null) {
            return ResponseEntity.badRequest().body(Map.of("error", passwordError));
        }

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
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(password))
                .activeRole(UserRole.PROFESSIONAL)
                .location(location)
                .professionType(profession)
                .professionalTitle(professionalTitle)
                .reputationScore(0.0)
                .totalRatings(0)
                .searchable(true)
                .build();

        professional = appUserRepo.save(professional);

        String token = jwtService.generateToken(
                professional.getId(),
                "PROFESSIONAL",
                professional.getEmail(),
                professional.getName()
        );

        return ResponseEntity.ok(Map.of(
                "token", token,
                "id", professional.getId(),
                "email", professional.getEmail(),
                "name", professional.getName(),
                "reputationScore", 0.0,
                "totalRatings", 0
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");

        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email y password son requeridos"));
        }

        Optional<AppUser> userOpt = appUserRepo.findByEmail(email);

        if (userOpt.isEmpty() || !UserRole.PROFESSIONAL.equals(userOpt.get().getActiveRole())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales inválidas"));
        }

        AppUser professional = userOpt.get();

        if (!passwordEncoder.matches(password, professional.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales inválidas"));
        }

        String token = jwtService.generateToken(
                professional.getId(),
                "PROFESSIONAL",
                professional.getEmail(),
                professional.getName()
        );

        double reputationScore = professional.getReputationScore() != null ? professional.getReputationScore() : 0.0;
        int totalRatings = professional.getTotalRatings() != null ? professional.getTotalRatings() : 0;

        return ResponseEntity.ok(Map.of(
                "token", token,
                "id", professional.getId(),
                "email", professional.getEmail(),
                "name", professional.getName(),
                "reputationScore", reputationScore,
                "totalRatings", totalRatings
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("message", "Logout exitoso"));
    }

    /**
     * Eliminar cuenta — SIEMPRE usa el userId del token JWT.
     * El {userId} del path se valida contra el token para evitar IDOR.
     */
    @DeleteMapping("/delete-account/{userId}")
    public ResponseEntity<?> deleteAccount(@PathVariable Long userId, HttpServletRequest request) {
        Long authenticatedUserId = (Long) request.getAttribute("userId");

        // SEGURIDAD: Si no hay usuario autenticado, rechazar inmediatamente
        if (authenticatedUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No autenticado"));
        }

        // SEGURIDAD: Solo permitir que el usuario elimine su propia cuenta
        if (!authenticatedUserId.equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No tenés permiso para eliminar esta cuenta"));
        }

        try {
            Optional<AppUser> userOpt = appUserRepo.findById(authenticatedUserId);

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Usuario no encontrado"));
            }

            AppUser user = userOpt.get();

            if (UserRole.PROFESSIONAL.equals(user.getActiveRole())) {
                if (user.getCv() != null) {
                    cvRepo.delete(user.getCv());
                }

                List<Rating> ratings = ratingRepo.findByProfessionalId(authenticatedUserId);
                ratingRepo.deleteAll(ratings);

                List<QrToken> qrCodes = qrCodeRepo.findByProfessionalId(authenticatedUserId);
                qrCodeRepo.deleteAll(qrCodes);
            } else if (UserRole.CLIENT.equals(user.getActiveRole())) {
                List<Rating> ratingsEmitted = ratingRepo.findByClientId(authenticatedUserId);
                ratingRepo.deleteAll(ratingsEmitted);
            }

            appUserRepo.deleteById(authenticatedUserId);

            return ResponseEntity.ok(Map.of("message", "Cuenta eliminada exitosamente"));

        } catch (Exception e) {
            System.err.println("Error eliminando cuenta: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al eliminar cuenta: " + e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String userType = (String) request.getAttribute("userType");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No autenticado"));
        }

        if (!"PROFESSIONAL".equals(userType)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Solo profesionales pueden acceder"));
        }

        Optional<AppUser> userOpt = appUserRepo.findById(userId);

        if (userOpt.isEmpty() || !UserRole.PROFESSIONAL.equals(userOpt.get().getActiveRole())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Profesional no encontrado"));
        }

        AppUser professional = userOpt.get();

        double reputationScore = professional.getReputationScore() != null ? professional.getReputationScore() : 0.0;
        int totalRatings = professional.getTotalRatings() != null ? professional.getTotalRatings() : 0;

        return ResponseEntity.ok(Map.of(
                "id", professional.getId(),
                "email", professional.getEmail(),
                "name", professional.getName(),
                "phone", professional.getPhone() != null ? professional.getPhone() : "",
                "location", professional.getLocation() != null ? professional.getLocation() : "",
                "professionalTitle", professional.getProfessionalTitle() != null ? professional.getProfessionalTitle() : "",
                "profilePicture", professional.getProfilePicture() != null ? professional.getProfilePicture() : "",
                "reputationScore", reputationScore,
                "totalRatings", totalRatings
        ));
    }

    @GetMapping("/me/client")
    public ResponseEntity<?> getCurrentClient(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String userType = (String) request.getAttribute("userType");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No autenticado"));
        }

        if (!"CLIENT".equals(userType)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Este endpoint es solo para clientes"));
        }

        Optional<AppUser> userOpt = appUserRepo.findById(userId);

        if (userOpt.isEmpty() || !UserRole.CLIENT.equals(userOpt.get().getActiveRole())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Cliente no encontrado"));
        }

        AppUser client = userOpt.get();

        return ResponseEntity.ok(Map.of(
                "id", client.getId(),
                "email", client.getEmail(),
                "name", client.getName()
        ));
    }

    @PutMapping("/update-profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> updates, HttpServletRequest request) {
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

        if ("PROFESSIONAL".equals(userType) && updates.containsKey("professionalTitle")) {
            user.setProfessionalTitle(updates.get("professionalTitle"));
        }

        appUserRepo.save(user);

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "phone", user.getPhone() != null ? user.getPhone() : "",
                "location", user.getLocation() != null ? user.getLocation() : "",
                "professionalTitle", user.getProfessionalTitle() != null ? user.getProfessionalTitle() : ""
        ));
    }

    @PostMapping("/upload-photo")
    public ResponseEntity<?> uploadPhoto(@RequestParam("photo") MultipartFile file, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String userType = (String) request.getAttribute("userType");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No autenticado"));
        }

        if (!"PROFESSIONAL".equals(userType)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Solo profesionales pueden subir fotos"));
        }

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se seleccionó ningún archivo"));
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Solo se permiten imágenes"));
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(Map.of("error", "La imagen no puede superar 5MB"));
        }

        try {
            Optional<AppUser> userOpt = appUserRepo.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Profesional no encontrado"));
            }
            AppUser professional = userOpt.get();

            String filename = userId + ".jpg";
            Path filepath = Paths.get(UPLOAD_DIR + filename);

            Files.deleteIfExists(filepath);

            Thumbnails.of(file.getInputStream())
                    .size(300, 300)
                    .outputFormat("jpg")
                    .toFile(filepath.toFile());

            String relativePath = "/" + UPLOAD_DIR + filename;
            professional.setProfilePicture(relativePath);
            appUserRepo.save(professional);

            return ResponseEntity.ok(Map.of(
                    "message", "Foto subida exitosamente",
                    "profilePicture", relativePath
            ));

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al guardar la imagen"));
        }
    }

    @PostMapping("/register-client")
    public ResponseEntity<?> registerClient(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");
        String name = request.get("name");

        if (email == null || password == null || name == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email, password y nombre son requeridos"));
        }

        String passwordError = validatePassword(password);
        if (passwordError != null) {
            return ResponseEntity.badRequest().body(Map.of("error", passwordError));
        }

        if (appUserRepo.findByEmail(email).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "El email ya está registrado"));
        }

        AppUser client = AppUser.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(password))
                .activeRole(UserRole.CLIENT)
                .build();

        client = appUserRepo.save(client);

        String token = jwtService.generateToken(
                client.getId(),
                "CLIENT",
                client.getEmail(),
                client.getName()
        );

        return ResponseEntity.ok(Map.of(
                "token", token,
                "id", client.getId(),
                "email", client.getEmail(),
                "name", client.getName()
        ));
    }

    @PostMapping("/login-client")
    public ResponseEntity<?> loginClient(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");

        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email y password son requeridos"));
        }

        Optional<AppUser> userOpt = appUserRepo.findByEmail(email);

        if (userOpt.isEmpty() || !UserRole.CLIENT.equals(userOpt.get().getActiveRole())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales inválidas"));
        }

        AppUser client = userOpt.get();

        if (!passwordEncoder.matches(password, client.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales inválidas"));
        }

        String token = jwtService.generateToken(
                client.getId(),
                "CLIENT",
                client.getEmail(),
                client.getName()
        );

        return ResponseEntity.ok(Map.of(
                "token", token,
                "id", client.getId(),
                "email", client.getEmail(),
                "name", client.getName()
        ));
    }

    /**
     * Eliminar cuenta de cliente — SIEMPRE usa el userId del token JWT.
     */
    @DeleteMapping("/delete-account-client/{userId}")
    public ResponseEntity<?> deleteClientAccount(@PathVariable Long userId, HttpServletRequest request) {
        Long authenticatedUserId = (Long) request.getAttribute("userId");

        // SEGURIDAD: Si no hay usuario autenticado, rechazar inmediatamente
        if (authenticatedUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No autenticado"));
        }

        // SEGURIDAD: Solo permitir que el usuario elimine su propia cuenta
        if (!authenticatedUserId.equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No tenés permiso para eliminar esta cuenta"));
        }

        Optional<AppUser> userOpt = appUserRepo.findById(authenticatedUserId);

        if (userOpt.isEmpty() || !UserRole.CLIENT.equals(userOpt.get().getActiveRole())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Cliente no encontrado"));
        }

        appUserRepo.deleteById(authenticatedUserId);

        return ResponseEntity.ok(Map.of("message", "Cuenta eliminada exitosamente"));
    }

    /**
     * Valida que el password cumpla requisitos mínimos de seguridad.
     * @return mensaje de error, o null si es válido.
     */
    private String validatePassword(String password) {
        if (password.length() < 8) {
            return "La contraseña debe tener al menos 8 caracteres";
        }
        if (!password.matches(".*[a-zA-Z].*")) {
            return "La contraseña debe contener al menos una letra";
        }
        if (!password.matches(".*[0-9].*")) {
            return "La contraseña debe contener al menos un número";
        }
        return null;
    }
}