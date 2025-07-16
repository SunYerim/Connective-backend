package com.connective.server.user.infrastructure.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtCookieUtil {

    @Value("${jwt.refresh-token-expiry}")
    private long refreshTokenExpiry;

    /**
     * Refresh Token을 HTTP Only 쿠키로 응답에 추가합니다.
     *
     * @param response     HttpServletResponse 객체
     * @param refreshToken Refresh Token 문자열
     */
    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refreshToken", refreshToken);

        cookie.setHttpOnly(true);
        cookie.setSecure(false); // 운영환경 - true
        cookie.setPath("/");
        cookie.setMaxAge((int) refreshTokenExpiry / 1000);

        response.addCookie(cookie);
    }

    /**
     * Refresh Token 쿠키를 삭제합니다. (로그아웃 시 등)
     *
     * @param response HttpServletResponse 객체
     */
    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

}
