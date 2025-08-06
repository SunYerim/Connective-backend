package com.connective.server.user.application.service;

import com.connective.server.user.domain.dto.auth.LoginResponseDTO;
import com.connective.server.user.domain.dto.auth.TokenDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

    LoginResponseDTO handleGoogleLogin(String code, HttpServletResponse response);

    TokenDTO reissueTokens(HttpServletRequest request, HttpServletResponse response);

    void logout(HttpServletRequest request, HttpServletResponse response);
}
