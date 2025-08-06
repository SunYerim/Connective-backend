package com.connective.server.user.presentation;

import com.connective.server.user.application.service.AuthService;
import com.connective.server.user.domain.dto.auth.LoginResponseDTO;
import com.connective.server.user.domain.dto.auth.TokenDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/google/callback")
    public ResponseEntity<LoginResponseDTO> googleCallback(@RequestParam String code,
        HttpServletResponse response) {
        if (code == null || code.isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }

        // 인가 코드를 사용하여 구글에 엑세스 토큰을 요청하는 로직을 서비스 계층으로 위임
        try {
            LoginResponseDTO loginResponse = authService.handleGoogleLogin(code,
                response); // 서비스 메서드 호출
            return ResponseEntity.ok(loginResponse); // 서비스에서 받은 결과 반환
        } catch (Exception e) {
            // 실제 서비스에서는 더 구체적인 예외 처리 및 에러 메시지 반환 필요
            System.err.println("Error during Google login: " + e.getMessage());
            return ResponseEntity.status(500)
                .body(null);
        }
    }

    @PostMapping("/reissue")
    public ResponseEntity<TokenDTO> reissueTokens(HttpServletRequest request,
        HttpServletResponse response) {
        try {
            TokenDTO newTokens = authService.reissueTokens(request, response);
            return ResponseEntity.ok(newTokens);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            authService.logout(request, response);
            return ResponseEntity.ok("Logout successful.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to logout.");
        }
    }


}
