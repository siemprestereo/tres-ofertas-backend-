package com.example.waiter_rating.dto.response;

import com.example.waiter_rating.model.ProfessionType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AppUserResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String location;
    private String professionalTitle;
    private String userType; // "CLIENT" o "PROFESSIONAL"
    private String activeRole; // "CLIENT" o "PROFESSIONAL"
    private ProfessionType professionType; // Solo presente si es PROFESSIONAL
    private String profilePicture;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}