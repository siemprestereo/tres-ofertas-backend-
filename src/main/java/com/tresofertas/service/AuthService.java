package com.tresofertas.service;

import com.tresofertas.dto.request.ForgotPasswordRequest;
import com.tresofertas.dto.request.LoginRequest;
import com.tresofertas.dto.request.RegisterRequest;
import com.tresofertas.dto.request.ResetPasswordRequest;
import com.tresofertas.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
}
