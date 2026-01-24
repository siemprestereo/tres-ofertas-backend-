package com.example.waiter_rating.controller;

import com.example.waiter_rating.model.*;
import com.example.waiter_rating.repository.*;
import com.example.waiter_rating.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final ProfessionalRepo professionalRepository;
    private final PasswordEncoder passwordEncoder;
    private final ClientRepo clientRepo;
    private final CvRepo cvRepo;
    private final RatingRepo ratingRepo;
    private final QrTokenRepo qrCodeRepo;

    private final AppUserRepo appUserRepo;
    private final JwtService jwtService;

    // Directorio para guardar fotos de perfil
    private static final String UPLOAD_DIR = "uploads/profiles/";

    public AuthController(ProfessionalRepo professionalRepository,
                          PasswordEncoder passwordEncoder,
                          ClientRepo clientRepo, CvRepo cvRepo, RatingRepo ratingRepo, QrTokenRepo qrCodeRepo, AppUserRepo appUserRepo,
                          JwtService jwtService) {
        this.professionalRepository = professionalRepository;
        this.passwordEncoder = passwordEncoder;
        this.clientRepo = clientRepo;
        this.cvRepo = cvRepo;
        this.ratingRepo = ratingRepo;
        this.qrCodeRepo = qrCodeRepo;
        this.appUserRepo = appUserRepo;
        this.jwtService = jwtService;

        // Crear directorio si no existe
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");
        String name = request.get("name");
        String professionType = request.get("professionType");
        String professionalTitle = request.get("professionalTitle");

        // ✅ Validación: professionType es obligatorio
        if (email == null || password == null || name == null || professionType == null || professionType.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email, password, nombre y tipo de profesión son requeridos"));
        }

        if (professionalRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "El email ya está registrado"));
        }

        Professional professional = new Professional();
        professional.setName(name);
        professional.setEmail(email);
        professional.setPassword(passwordEncoder.encode(password));

        // ✅ Guardar professionType (obligatorio con validación)
        try {
            professional.setProfessionType(ProfessionType.valueOf(professionType));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Tipo de profesión inválido: " + professionType));
        }

        // ✅ Guardar professionalTitle (opcional)
        if (professionalTitle != null && !professionalTitle.isEmpty()) {
            professional.setProfessionalTitle(professionalTitle);
        }

        professionalRepository.save(professional);

        // Generar JWT
        String token = jwtService.generateToken(
                professional.getId(),
                "PROFESSIONAL",
                professional.getEmail(),
                professional.getName()
        );

        double reputationScore = professional.getReputationScore() != null
                ? professional.getReputationScore()
                : 0.0;
        int totalRatings = professional.getTotalRatings() != null
                ? professional.getTotalRatings()
                : 0;

        return ResponseEntity.ok(Map.of(
                "token", token,
                "id", professional.getId(),
                "email", professional.getEmail(),
                "name", professional.getName(),
                "reputationScore", reputationScore,
                "totalRatings", totalRatings
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
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

        // Generar JWT
        String token = jwtService.generateToken(
                professional.getId(),
                "PROFESSIONAL",
                professional.getEmail(),
                professional.getName()
        );

        double reputationScore = professional.getReputationScore() != null
                ? professional.getReputationScore()
                : 0.0;
        int totalRatings = professional.getTotalRatings() != null
                ? professional.getTotalRatings()
                : 0;

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
        // Con JWT, el logout se maneja en el frontend eliminando el token
        return ResponseEntity.ok(Map.of("message", "Logout exitoso"));
    }

    @DeleteMapping("/delete-account/{userId}")
    public ResponseEntity<?> deleteAccount(@PathVariable Long userId, HttpServletRequest request) {
        try {
            // Obtener userId del JWT (guardado por el filtro)
            Long authenticatedUserId = (Long) request.getAttribute("userId");

            // Usar el userId autenticado si existe, sino usar el del path
            Long userToDelete = authenticatedUserId != null ? authenticatedUserId : userId;

            // Buscar el usuario en app_users
            Optional<AppUser> userOpt = appUserRepo.findById(userToDelete);

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Usuario no encontrado"));
            }

            AppUser user = userOpt.get();

            // Determinar el tipo de usuario y eliminarlo
            if (user instanceof Professional) {
                Professional professional = (Professional) user;

                // Eliminar CV si existe (cascade debería manejarlo, pero por si acaso)
                if (professional.getCv() != null) {
                    cvRepo.delete(professional.getCv());
                }

                // Eliminar ratings recibidos
                List<Rating> ratings = ratingRepo.findByProfessionalId(userToDelete);
                ratingRepo.deleteAll(ratings);

                // Eliminar QRs
                List<QrToken> qrCodes = qrCodeRepo.findByProfessionalId(userToDelete);
                qrCodeRepo.deleteAll(qrCodes);

                // Eliminar el profesional (esto eliminará también de app_users por herencia)
                professionalRepository.deleteById(userToDelete);

            } else if (user instanceof Client) {
                Client client = (Client) user;

                // Eliminar ratings emitidos por el cliente
                List<Rating> ratingsEmitted = ratingRepo.findByClientId(userToDelete);
                ratingRepo.deleteAll(ratingsEmitted);

                // Eliminar el cliente (esto eliminará también de app_users por herencia)
                clientRepo.deleteById(userToDelete);

            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Tipo de usuario no reconocido"));
            }

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
        // Obtener userId y userType del JWT (guardados por el filtro)
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

    @GetMapping("/me/client")
    public ResponseEntity<?> getCurrentClient(HttpServletRequest request) {
        // Obtener userId y userType del JWT (guardados por el filtro)
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

        Optional<Client> clientOpt = clientRepo.findById(userId);

        if (clientOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Cliente no encontrado"));
        }

        Client client = clientOpt.get();

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

        // --- LÓGICA PARA PROFESIONAL ---
        if ("PROFESSIONAL".equals(userType)) {
            Optional<Professional> professionalOpt = professionalRepository.findById(userId);
            if (professionalOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Profesional no encontrado"));

            Professional p = professionalOpt.get();
            if (updates.containsKey("phone")) p.setPhone(updates.get("phone"));
            if (updates.containsKey("location")) p.setLocation(updates.get("location"));
            if (updates.containsKey("professionalTitle")) p.setProfessionalTitle(updates.get("professionalTitle"));

            professionalRepository.save(p);
            return ResponseEntity.ok(p); // Retornamos el objeto actualizado
        }

        // --- LÓGICA PARA CLIENTE ---
        else if ("CLIENT".equals(userType)) {
            Optional<Client> clientOpt = clientRepo.findById(userId);
            if (clientOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Cliente no encontrado"));

            Client c = clientOpt.get();
            if (updates.containsKey("phone")) c.setPhone(updates.get("phone"));
            if (updates.containsKey("location")) c.setLocation(updates.get("location"));

            clientRepo.save(c);
            return ResponseEntity.ok(c); // Retornamos el objeto actualizado
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Tipo de usuario no válido"));
    }

    @PostMapping("/upload-photo")
    public ResponseEntity<?> uploadPhoto(@RequestParam("photo") MultipartFile file, HttpServletRequest request) {
        // Obtener userId y userType del JWT (guardados por el filtro)
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
    public ResponseEntity<?> registerClient(@RequestBody Map<String, String> request) {
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

        // Generar JWT
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

        // Generar JWT
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

    @DeleteMapping("/delete-account-client/{userId}")
    public ResponseEntity<?> deleteClientAccount(@PathVariable Long userId, HttpServletRequest request) {
        // Obtener userId del JWT (guardado por el filtro)
        Long authenticatedUserId = (Long) request.getAttribute("userId");
        Long userToDelete = authenticatedUserId != null ? authenticatedUserId : userId;

        Optional<Client> clientOpt = clientRepo.findById(userToDelete);

        if (clientOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Cliente no encontrado"));
        }

        // Eliminar cliente (cascade eliminará ratings, etc.)
        clientRepo.deleteById(userToDelete);

        return ResponseEntity.ok(Map.of("message", "Cuenta eliminada exitosamente"));
    }
}