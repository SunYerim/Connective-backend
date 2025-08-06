package com.connective.server.user.infrastructure.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtCookieUtil {

    @Value("${jwt.refresh-token-expiry}")
    private long refreshTokenExpiry;

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken"; // 쿠키 이름 상수로 정의

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
     * HTTP 요청에서 Refresh Token 쿠키를 추출합니다. 이 메서드는 재발급 및 로그아웃 로직에 필요합니다.
     *
     * @param request HttpServletRequest 객체
     * @return 추출된 Refresh Token 문자열, 없으면 null
     */
    public String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                    log.debug("Found Refresh Token in cookie: {}", cookie.getName());
                    return cookie.getValue();
                }
            }
        }
        log.debug("Refresh Token cookie '{}' not found in request.", REFRESH_TOKEN_COOKIE_NAME);
        return null;
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
