package com.example.waiter_rating.service.impl;

import com.example.waiter_rating.model.AppUser;
import com.example.waiter_rating.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${resend.api-key}")
    private String apiKey;

    @Value("${resend.from}")
    private String defaultFrom;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${app.mail.suggestions:hola@calificalo.com.ar}")
    private String suggestionsEmail;

    private static final String RESEND_URL = "https://api.resend.com/emails";

    // ========== ENVÍO BASE ==========

    private void send(String from, String to, String subject, String html) {
        send(from, to, subject, html, null);
    }

    private void send(String from, String to, String subject, String html, String replyTo) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            var body = new java.util.HashMap<String, Object>();
            body.put("from", from);
            body.put("to", List.of(to));
            body.put("subject", subject);
            body.put("html", html);
            if (replyTo != null) body.put("reply_to", List.of(replyTo));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(RESEND_URL, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Resend API error {}: {}", response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    // ========== MÉTODOS PÚBLICOS ==========

    @Override
    @Async
    public void sendWelcomeEmail(String toEmail, String userName, String role) {
        String html = "PROFESSIONAL".equals(role)
                ? buildWelcomeProfessionalEmailTemplate(userName)
                : buildWelcomeEmailTemplate(userName);
        send(defaultFrom, toEmail, "¡Bienvenido a Calificalo!", html);
        log.info("Welcome email sent to: {}", toEmail);
    }

    @Override
    @Async
    public void sendVerificationEmail(String toEmail, String userName, String token, String role) {
        String verificationUrl = frontendUrl + "/verify-email?token=" + token;
        String html = "PROFESSIONAL".equals(role)
                ? buildVerificationProfessionalTemplate(userName, verificationUrl)
                : buildVerificationClientTemplate(userName, verificationUrl);
        send(defaultFrom, toEmail, "¡Bienvenido a Calificalo! Verificá tu cuenta ✅", html);
        log.info("Verification email sent to: {}", toEmail);
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String toEmail, String userName, String token) {
        String resetUrl = frontendUrl + "/reset-password?token=" + token;
        send(defaultFrom, toEmail, "Recuperación de contraseña - Calificalo 🔐", buildPasswordResetEmailTemplate(userName, resetUrl));
        log.info("Password reset email sent to: {}", toEmail);
    }

    @Override
    @Async
    public void sendProfessionSuggestionEmail(String professionalName, String professionalEmail, String suggestion) {
        send(defaultFrom, suggestionsEmail, "Nueva sugerencia de profesión - Calificalo",
                buildProfessionSuggestionTemplate(suggestion, professionalName, professionalEmail));
        log.info("Profession suggestion email sent from: {}", professionalEmail);
    }

    @Override
    @Async
    public void sendAdminEmail(String toEmail, String toName, String subject, String body, String replyTo) {
        String from = "Calificalo <" + replyTo + ">";
        send(from, toEmail, subject, buildAdminEmailTemplate(toName, body, replyTo), replyTo);
        log.info("Admin email sent to: {}", toEmail);
    }

    @Override
    @Async
    public void sendSuggestionAcceptedEmail(String toEmail, String userName, String professionName) {
        String html = "<div style='font-family:sans-serif;max-width:600px;margin:0 auto;padding:24px'>"
            + "<h2 style='color:#7c3aed'>¡Tu sugerencia fue aceptada! 🎉</h2>"
            + "<p>Hola <strong>" + userName + "</strong>,</p>"
            + "<p>Nos complace informarte que tu sugerencia de profesión <strong>\"" + professionName + "\"</strong> fue agregada a Calificalo.</p>"
            + "<p>¡Gracias por ayudarnos a mejorar la plataforma!</p>"
            + "<p style='color:#6b7280;font-size:0.9em'>El equipo de Calificalo</p>"
            + "</div>";
        send(defaultFrom, toEmail, "¡Tu sugerencia fue agregada a Calificalo!", html);
        log.info("Suggestion accepted email sent to: {}", toEmail);
    }

    @Override
    @Async
    public void sendBroadcastEmail(List<AppUser> recipients, String subject, String body, String replyTo) {
        String from = "Calificalo <" + replyTo + ">";
        int sent = 0;
        for (AppUser user : recipients) {
            send(from, user.getEmail(), subject, buildAdminEmailTemplate(user.getName(), body, replyTo), replyTo);
            sent++;
        }
        log.info("Broadcast email sent to {}/{} recipients", sent, recipients.size());
    }

    // ========== TEMPLATES ==========

    private String buildAdminEmailTemplate(String recipientName, String body, String replyTo) {
        String bodyHtml = body.replace("\n", "<br>");
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.7; color: #333; margin: 0; padding: 0; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 28px 30px; border-radius: 10px 10px 0 0; }
                    .content { background: #ffffff; padding: 30px; border: 1px solid #e0e0e0; font-size: 15px; }
                    .footer { background: #f8f9fa; padding: 18px 20px; text-align: center; font-size: 12px; color: #888; border-radius: 0 0 10px 10px; border: 1px solid #e0e0e0; border-top: none; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header"><h2 style="margin:0; font-weight:300; font-size:22px;">Calificalo</h2></div>
                    <div class="content">
                        <p>Hola %s,</p>
                        <p>%s</p>
                    </div>
                    <div class="footer">
                        <p>© 2025 Calificalo. Todos los derechos reservados.</p>
                        <p>Podés responder este email a <a href="mailto:%s">%s</a></p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(recipientName, bodyHtml, replyTo, replyTo);
    }

    private String buildProfessionSuggestionTemplate(String suggestion, String professionalName, String professionalEmail) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 24px 30px; border-radius: 10px 10px 0 0; }
                    .content { background: #ffffff; padding: 30px; border: 1px solid #e0e0e0; }
                    .suggestion-box { background: #f3f4f6; border-left: 4px solid #667eea; padding: 16px 20px; border-radius: 0 8px 8px 0; margin: 20px 0; font-size: 18px; font-weight: bold; color: #1f2937; }
                    .meta { font-size: 13px; color: #6b7280; margin-top: 20px; }
                    .footer { background: #f8f9fa; padding: 16px 20px; text-align: center; font-size: 12px; color: #666; border-radius: 0 0 10px 10px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header"><h2 style="margin:0">💡 Nueva sugerencia de profesión</h2></div>
                    <div class="content">
                        <p>Un profesional sugirió agregar una nueva categoría a Calificalo:</p>
                        <div class="suggestion-box">%s</div>
                        <div class="meta">
                            <strong>Enviado por:</strong> %s<br>
                            <strong>Email:</strong> %s
                        </div>
                    </div>
                    <div class="footer"><p>© 2025 Calificalo. Panel interno.</p></div>
                </div>
            </body>
            </html>
            """.formatted(suggestion, professionalName, professionalEmail);
    }

    private String buildWelcomeEmailTemplate(String userName) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #ffffff; padding: 30px; border: 1px solid #e0e0e0; }
                    .footer { background: #f8f9fa; padding: 20px; text-align: center; font-size: 12px; color: #666; border-radius: 0 0 10px 10px; }
                    .button { display: inline-block; padding: 12px 30px; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header"><h1>¡Bienvenido a Calificalo!</h1></div>
                    <div class="content">
                        <h2>Hola %s,</h2>
                        <p>Nos alegra tenerte con nosotros. Tu cuenta ha sido creada exitosamente.</p>
                        <p>Con Calificalo podrás calificar profesionales de manera rápida y segura, ver tu historial de calificaciones y descubrir los profesionales mejor valorados.</p>
                        <a href="%s" class="button">Ir a Calificalo</a>
                    </div>
                    <div class="footer"><p>© 2025 Calificalo. Todos los derechos reservados.</p></div>
                </div>
            </body>
            </html>
            """.formatted(userName, frontendUrl);
    }

    private String buildVerificationClientTemplate(String userName, String verificationUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #10b981 0%%, #0d9488 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #ffffff; padding: 30px; border: 1px solid #e0e0e0; }
                    .footer { background: #f8f9fa; padding: 20px; text-align: center; font-size: 12px; color: #666; border-radius: 0 0 10px 10px; }
                    .button { display: inline-block; padding: 14px 36px; background: linear-gradient(135deg, #10b981 0%%, #0d9488 100%%); color: white; text-decoration: none; border-radius: 8px; margin: 20px 0; font-weight: bold; font-size: 16px; }
                    .note { background: #f0fdf4; border-left: 4px solid #10b981; padding: 14px 18px; margin: 20px 0; border-radius: 0 8px 8px 0; font-size: 13px; color: #065f46; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header"><h1 style="margin:0">¡Bienvenido a Calificalo!</h1></div>
                    <div class="content">
                        <h2>Hola %s,</h2>
                        <p>Tu cuenta fue creada exitosamente. Con Calificalo podés calificar profesionales, ver tu historial de calificaciones y descubrir los profesionales mejor valorados.</p>
                        <p>Solo falta un paso: verificá tu correo para activar tu cuenta.</p>
                        <p style="text-align: center;"><a href="%s" class="button">Verificar mi cuenta</a></p>
                        <div class="note"><strong>⏰ Este enlace expira en 24 horas.</strong> Si no te registraste en Calificalo, podés ignorar este correo.</div>
                    </div>
                    <div class="footer"><p>© 2025 Calificalo. Todos los derechos reservados.</p></div>
                </div>
            </body>
            </html>
            """.formatted(userName, verificationUrl);
    }

    private String buildVerificationProfessionalTemplate(String userName, String verificationUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #ffffff; padding: 30px; border: 1px solid #e0e0e0; }
                    .footer { background: #f8f9fa; padding: 20px; text-align: center; font-size: 12px; color: #666; border-radius: 0 0 10px 10px; }
                    .button { display: inline-block; padding: 14px 36px; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; text-decoration: none; border-radius: 8px; margin: 20px 0; font-weight: bold; font-size: 16px; }
                    .highlight { background: #f3f4f6; border-left: 4px solid #667eea; padding: 14px 18px; border-radius: 0 8px 8px 0; margin: 20px 0; }
                    .note { background: #faf5ff; border-left: 4px solid #764ba2; padding: 14px 18px; margin: 20px 0; border-radius: 0 8px 8px 0; font-size: 13px; color: #4c1d95; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header"><h1 style="margin:0">¡Bienvenido a Calificalo!</h1></div>
                    <div class="content">
                        <h2>Hola %s,</h2>
                        <p>Tu perfil profesional fue creado exitosamente. A partir de ahora podés recibir calificaciones de tus clientes y construir tu reputación en la plataforma.</p>
                        <div class="highlight">
                            <strong>¿Qué podés hacer en Calificalo?</strong>
                            <ul style="margin: 8px 0 0; padding-left: 20px;">
                                <li>Recibir calificaciones y reseñas de tus clientes</li>
                                <li>Armar tu CV digital con tu historial de calificaciones</li>
                                <li>Mostrar tu reputación para conseguir más y mejores oportunidades laborales</li>
                            </ul>
                        </div>
                        <p>Solo falta un paso: verificá tu correo para activar tu cuenta.</p>
                        <p style="text-align: center;"><a href="%s" class="button">Verificar mi cuenta</a></p>
                        <div class="note"><strong>⏰ Este enlace expira en 24 horas.</strong> Si no te registraste en Calificalo, podés ignorar este correo.</div>
                    </div>
                    <div class="footer"><p>© 2025 Calificalo. Todos los derechos reservados.</p></div>
                </div>
            </body>
            </html>
            """.formatted(userName, verificationUrl);
    }

    private String buildWelcomeProfessionalEmailTemplate(String userName) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #ffffff; padding: 30px; border: 1px solid #e0e0e0; }
                    .footer { background: #f8f9fa; padding: 20px; text-align: center; font-size: 12px; color: #666; border-radius: 0 0 10px 10px; }
                    .button { display: inline-block; padding: 12px 30px; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .highlight { background: #f3f4f6; border-left: 4px solid #667eea; padding: 14px 18px; border-radius: 0 8px 8px 0; margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header"><h1>¡Bienvenido a Calificalo!</h1></div>
                    <div class="content">
                        <h2>Hola %s,</h2>
                        <p>Tu perfil profesional fue creado exitosamente. A partir de ahora podés empezar a recibir calificaciones de tus clientes.</p>
                        <div class="highlight">
                            <strong>¿Qué podés hacer en Calificalo?</strong>
                            <ul style="margin: 10px 0 0; padding-left: 20px;">
                                <li>Recibir calificaciones y reseñas de tus clientes</li>
                                <li>Armar tu CV digital con tu historial de calificaciones</li>
                                <li>Mostrar tu reputación para conseguir más y mejores oportunidades laborales</li>
                            </ul>
                        </div>
                        <p>Completá tu CV ahora para que tus clientes puedan encontrarte y calificarte.</p>
                        <a href="%s/edit-cv" class="button">Completar mi CV</a>
                    </div>
                    <div class="footer"><p>© 2025 Calificalo. Todos los derechos reservados.</p></div>
                </div>
            </body>
            </html>
            """.formatted(userName, frontendUrl);
    }

    private String buildPasswordResetEmailTemplate(String userName, String resetUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #ffffff; padding: 30px; border: 1px solid #e0e0e0; }
                    .footer { background: #f8f9fa; padding: 20px; text-align: center; font-size: 12px; color: #666; border-radius: 0 0 10px 10px; }
                    .button { display: inline-block; padding: 12px 30px; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .warning { background: #f8d7da; border-left: 4px solid #dc3545; padding: 15px; margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header"><h1>Recuperación de contraseña</h1></div>
                    <div class="content">
                        <h2>Hola %s,</h2>
                        <p>Recibimos una solicitud para restablecer tu contraseña. Hacé clic en el botón para crear una nueva:</p>
                        <p style="text-align: center;"><a href="%s" class="button">Restablecer contraseña</a></p>
                        <div class="warning">
                            <strong>🔒 Seguridad:</strong> Este enlace expirará en 2 horas.<br>
                            Si no solicitaste este cambio, ignorá este correo.
                        </div>
                    </div>
                    <div class="footer"><p>© 2025 Calificalo. Todos los derechos reservados.</p></div>
                </div>
            </body>
            </html>
            """.formatted(userName, resetUrl);
    }
}
