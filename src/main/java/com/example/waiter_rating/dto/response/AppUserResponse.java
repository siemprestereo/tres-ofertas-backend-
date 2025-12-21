package com.example.waiter_rating.dto.response;

import com.example.waiter_rating.model.ProfessionType;
import lombok.Data;

@Data
public class AppUserResponse {
    private Long id;
    private String name;
    private String email;
    private String userType; // "CLIENT" o "PROFESSIONAL"
    private ProfessionType professionType; // Solo presente si es PROFESSIONAL
    private String profilePicture;
}