package com.example.waiter_rating.service;

import com.example.waiter_rating.model.AppUser;
import com.example.waiter_rating.model.UserRole;

import java.util.List;

public interface RoleSwitchService {
    AppUser switchRole(Long userId, UserRole newRole, List<String> professionTypes, String professionalTitle);
}
