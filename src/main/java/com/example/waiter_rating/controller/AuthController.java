package com.example.waiter_rating.controller;

import com.example.waiter_rating.model.Client;
import com.example.waiter_rating.model.Professional;
import com.example.waiter_rating.repository.ClientRepo;
import com.example.waiter_rating.repository.ProfessionalRepo;
import jakarta.servlet.http.HttpSession;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final ProfessionalRepo professionalRepository;
    private final PasswordEncoder passwordEncoder;

    private final ClientRepo clientRepo;

    // Directorio para guardar fotos de perfil
    private static final String UPLOAD_DIR = "uploads/profiles/";

    public AuthController(ProfessionalRepo professionalRepository, PasswordEncoder passwordEncoder) {
        this.professionalRepository = professionalRepository;
        this.passwordEncoder = passwordEncoder;

        // Crear directorio si no existe
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request, HttpSession session) {
        String email = request.get("email");
        String password = request.get("password");
        String name = request.get("name");

        if (email == null || password == null || name == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email, password y nombre son requeridos"));
        }

        if (professionalRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "El email ya está registrado"));
        }

        Professional professional = new Professional();
        professional.setName(name);
        professional.setEmail(email);
        professional.setPassword(passwordEncoder.encode(password));

        professionalRepository.save(professional);

        // Crear sesión HTTP
        session.setAttribute("userId", professional.getId());
        session.setAttribute("userType", "PROFESSIONAL");

        // Autenticar en Spring Security
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        double reputationScore = professional.getReputationScore() != null
                ? professional.getReputationScore()
                : 0.0;
        int totalRatings = professional.getTotalRatings() != null
                ? professional.getTotalRatings()
                : 0;

        return ResponseEntity.ok(Map.of(
                "id", professional.getId(),
                "email", professional.getEmail(),
                "name", professional.getName(),
                "reputationScore", reputationScore,
                "totalRatings", totalRatings
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request, HttpSession session) {
        String email = request.get("email");
        String password = request.get("password");

        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email y password son requeridos"));
        }

        Optional<Professional> professionalOpt = professionalRepository.findByEmail(email);

        if (professionalOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales inválidas"));
        }

        Professional professional = professionalOpt.get();

        if (!passwordEncoder.matches(password, professional.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales inválidas"));
        }

        // Crear sesión HTTP
        session.setAttribute("userId", professional.getId());
        session.setAttribute("userType", "PROFESSIONAL");

        // Autenticar in Spring Security
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        double reputationScore = professional.getReputationScore() != null
                ? professional.getReputationScore()
                : 0.0;
        int totalRatings = professional.getTotalRatings() != null
                ? professional.getTotalRatings()
                : 0;

        return ResponseEntity.ok(Map.of(
                "id", professional.getId(),
                "email", professional.getEmail(),
                "name", professional.getName(),
                "reputationScore", reputationScore,
                "totalRatings", totalRatings
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "Logout exitoso"));
    }

    @DeleteMapping("/delete-account/{userId}")
    public ResponseEntity<?> deleteAccount(@PathVariable Long userId, HttpSession session) {
        // Verificar si hay sesión (opcional para compatibilidad)
        Long sessionUserId = (Long) session.getAttribute("userId");

        // Si no hay sesión, usar el userId del path
        Long userToDelete = sessionUserId != null ? sessionUserId : userId;

        Optional<Professional> professionalOpt = professionalRepository.findById(userToDelete);

        if (professionalOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Profesional no encontrado"));
        }

        // Eliminar profesional (cascade eliminará CV, ratings, etc.)
        professionalRepository.deleteById(userToDelete);

        // Invalidar sesión si existe
        if (sessionUserId != null) {
            session.invalidate();
            SecurityContextHolder.clearContext();
        }

        return ResponseEntity.ok(Map.of("message", "Cuenta eliminada exitosamente"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        String userType = (String) session.getAttribute("userType");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No autenticado"));
        }

        if (!"PROFESSIONAL".equals(userType)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Solo profesionales pueden acceder"));
        }

        Optional<Professional> professionalOpt = professionalRepository.findById(userId);

        if (professionalOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Profesional no encontrado"));
        }

        Professional professional = professionalOpt.get();

        double reputationScore = professional.getReputationScore() != null
                ? professional.getReputationScore()
                : 0.0;
        int totalRatings = professional.getTotalRatings() != null
                ? professional.getTotalRatings()
                : 0;

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

    @PutMapping("/update-profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> updates, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        String userType = (String) session.getAttribute("userType");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No autenticado"));
        }

        if (!"PROFESSIONAL".equals(userType)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Solo profesionales pueden actualizar su perfil"));
        }

        Optional<Professional> professionalOpt = professionalRepository.findById(userId);

        if (professionalOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Profesional no encontrado"));
        }

        Professional professional = professionalOpt.get();

        // Actualizar campos opcionales
        if (updates.containsKey("phone")) {
            professional.setPhone(updates.get("phone"));
        }
        if (updates.containsKey("location")) {
            professional.setLocation(updates.get("location"));
        }
        if (updates.containsKey("professionalTitle")) {
            professional.setProfessionalTitle(updates.get("professionalTitle"));
        }

        professionalRepository.save(professional);

        return ResponseEntity.ok(Map.of(
                "message", "Perfil actualizado correctamente",
                "phone", professional.getPhone() != null ? professional.getPhone() : "",
                "location", professional.getLocation() != null ? professional.getLocation() : "",
                "professionalTitle", professional.getProfessionalTitle() != null ? professional.getProfessionalTitle() : ""
        ));
    }

    @PostMapping("/upload-photo")
    public ResponseEntity<?> uploadPhoto(@RequestParam("photo") MultipartFile file, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        String userType = (String) session.getAttribute("userType");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No autenticado"));
        }

        if (!"PROFESSIONAL".equals(userType)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Solo profesionales pueden subir fotos"));
        }

        // Validaciones
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se seleccionó ningún archivo"));
        }

        // Validar tipo de archivo
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Solo se permiten imágenes"));
        }

        // Validar tamaño (máx 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(Map.of("error", "La imagen no puede superar 5MB"));
        }

        try {
            // Obtener profesional
            Optional<Professional> professionalOpt = professionalRepository.findById(userId);
            if (professionalOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Profesional no encontrado"));
            }
            Professional professional = professionalOpt.get();

            // Nombre del archivo: userId.jpg
            String filename = userId + ".jpg";
            Path filepath = Paths.get(UPLOAD_DIR + filename);

            // Eliminar foto anterior si existe
            Files.deleteIfExists(filepath);

            // Redimensionar y guardar (300x300px)
            Thumbnails.of(file.getInputStream())
                    .size(300, 300)
                    .outputFormat("jpg")
                    .toFile(filepath.toFile());

            // Actualizar BD con la ruta relativa
            String relativePath = "/" + UPLOAD_DIR + filename;
            professional.setProfilePicture(relativePath);
            professionalRepository.save(professional);

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

    // ========== CLIENTES ==========

    @PostMapping("/register-client")
    public ResponseEntity<?> registerClient(@RequestBody Map<String, String> request, HttpSession session) {
        String email = request.get("email");
        String password = request.get("password");
        String name = request.get("name");

        if (email == null || password == null || name == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email, password y nombre son requeridos"));
        }

        if (clientRepo.findByEmail(email).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "El email ya está registrado"));
        }

        Client client = new Client();
        client.setName(name);
        client.setEmail(email);
        client.setPassword(passwordEncoder.encode(password));

        clientRepo.save(client);

        // Crear sesión HTTP
        session.setAttribute("userId", client.getId());
        session.setAttribute("userType", "CLIENT");

        // Autenticar en Spring Security
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        return ResponseEntity.ok(Map.of(
                "id", client.getId(),
                "email", client.getEmail(),
                "name", client.getName()
        ));
    }

    @PostMapping("/login-client")
    public ResponseEntity<?> loginClient(@RequestBody Map<String, String> request, HttpSession session) {
        String email = request.get("email");
        String password = request.get("password");

        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email y password son requeridos"));
        }

        Optional<Client> clientOpt = clientRepo.findByEmail(email);

        if (clientOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales inválidas"));
        }

        Client client = clientOpt.get();

        if (!passwordEncoder.matches(password, client.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales inválidas"));
        }

        // Crear sesión HTTP
        session.setAttribute("userId", client.getId());
        session.setAttribute("userType", "CLIENT");

        // Autenticar en Spring Security
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        return ResponseEntity.ok(Map.of(
                "id", client.getId(),
                "email", client.getEmail(),
                "name", client.getName()
        ));
    }

    @DeleteMapping("/delete-account-client/{userId}")
    public ResponseEntity<?> deleteClientAccount(@PathVariable Long userId, HttpSession session) {
        Long sessionUserId = (Long) session.getAttribute("userId");
        Long userToDelete = sessionUserId != null ? sessionUserId : userId;

        Optional<Client> clientOpt = clientRepo.findById(userToDelete);

        if (clientOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Cliente no encontrado"));
        }

        // Eliminar cliente (cascade eliminará ratings, etc.)
        clientRepo.deleteById(userToDelete);

        // Invalidar sesión si existe
        if (sessionUserId != null) {
            session.invalidate();
            SecurityContextHolder.clearContext();
        }

        return ResponseEntity.ok(Map.of("message", "Cuenta eliminada exitosamente"));
    }


}