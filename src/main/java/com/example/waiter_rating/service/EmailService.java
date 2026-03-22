package com.example.waiter_rating.service;

public interface EmailService {
    void sendWelcomeEmail(String toEmail, String userName, String role);
    void sendVerificationEmail(String toEmail, String userName, String token, String role);
    void sendPasswordResetEmail(String toEmail, String userName, String token);
    void sendProfessionSuggestionEmail(String professionalName, String professionalEmail, String suggestion);
    void sendAdminEmail(String toEmail, String toName, String subject, String body, String replyTo);
    void sendBroadcastEmail(java.util.List<com.example.waiter_rating.model.AppUser> recipients, String subject, String body, String replyTo);
    void sendSuggestionAcceptedEmail(String toEmail, String userName, String professionName);
}
