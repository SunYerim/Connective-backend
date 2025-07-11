package com.connective.server.user.presentation;


import com.connective.server.user.application.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/google")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping
    public ResponseEntity<String> googleCallback(@RequestParam String code) {
        if (code == null || code.isEmpty()) {
            return ResponseEntity.badRequest().body("Authorization code not found.");
        }

        // 인가 코드를 사용하여 구글에 엑세스 토큰을 요청하는 로직을 서비스 계층으로 위임
        try {
            String result = authService.handleGoogleLogin(code); // 서비스 메서드 호출
            return ResponseEntity.ok(result); // 서비스에서 받은 결과 반환
        } catch (Exception e) {
            // 실제 서비스에서는 더 구체적인 예외 처리 및 에러 메시지 반환 필요
            System.err.println("Error during Google login: " + e.getMessage());
            return ResponseEntity.status(500)
                .body("Google login failed due to an internal server error.");
        }
    }

}
