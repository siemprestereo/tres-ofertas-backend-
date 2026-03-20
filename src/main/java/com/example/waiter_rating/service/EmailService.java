package com.example.waiter_rating.service;

public interface EmailService {
    void sendWelcomeEmail(String toEmail, String userName);
    void sendVerificationEmail(String toEmail, String userName, String token);
    void sendPasswordResetEmail(String toEmail, String userName, String token);
    void sendProfessionSuggestionEmail(String professionalName, String professionalEmail, String suggestion);
}
