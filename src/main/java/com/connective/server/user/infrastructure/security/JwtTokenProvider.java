package com.connective.server.user.infrastructure.security;

import com.connective.server.user.domain.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiry}")
    private long accessTokenExpiry;

    @Value("${jwt.refresh-token-expiry}")
    private long refreshTokenExpiry;

    private Key key;

    @PostConstruct
    public void init() {
        // secretKey 문자열을 바이트 배열로 변환하여 Key 객체 생성
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        log.info("JWT Secret Key initialized.");
    }

    /**
     * Access Token 생성
     *
     * @param user 인증된 사용자 정보
     * @return 생성된 Access Token 문자열
     */
    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("email", user.getEmail());

        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessTokenExpiry);

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(String.valueOf(user.getId()))
            .setIssuedAt(now)
            .setExpiration(expiration)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    /**
     * Refresh Token 생성 Refresh Token은 특별한 클레임 없이 만료 시간만 길게 설정
     *
     * @return 생성된 Refresh Token 문자열
     */
    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + refreshTokenExpiry);

        return Jwts.builder()
            .setSubject(String.valueOf(userId))
            .setIssuedAt(now)
            .setExpiration(expiration)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    /**
     * 토큰 유효성 검증
     *
     * @param token 검증할 토큰
     * @return 토큰이 유효하면 true, 아니면 false
     */
    // JWT 토큰의 유효성을 검증
    public boolean validateToken(String token) {

        try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.info("Invalid JWT Token", e);
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT Token", e);
        } catch (UnsupportedJwtException e) {
            log.info("Unsupported JWT Token", e);
        } catch (IllegalArgumentException e) {
            log.info("JWT claims string is empty.", e);
        }
        return false;
    }

    /**
     * 토큰에서 클레임 정보 추출 (주로 JwtAuthenticationFilter에서 토큰의 사용자 ID 등을 얻을 때 사용)
     *
     * @param token 토큰 문자열
     * @return 토큰의 Claims 객체
     */
    public Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token).
                getBody();
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT Token. Claims can still be retrieved.", e);
            return e.getClaims();
        } catch (SecurityException | MalformedJwtException | UnsupportedJwtException |
                 IllegalArgumentException e) {
            // TODO: 커스텀예외 던지기
            log.error("Invalid or malformed JWT Token. Cannot parse claims.", e);
            return null;
        }
    }

    /**
     * 토큰에서 사용자 ID(Subject)를 추출합니다.
     * @param token 토큰 문자열
     * @return 사용자 ID (Long), 추출 실패 시 null
     */
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = parseClaims(token);
            if (claims != null && claims.getSubject() != null) {
                return Long.valueOf(claims.getSubject());
            }
        } catch (Exception e) {
            log.error("Failed to extract userId from token: {}", e.getMessage());
        }
        return null;
    }




    /**
     * Access Token 만료 시간(밀리초)을 반환합니다.
     *
     * @return accessTokenExpiry
     */
    public long getAccessTokenExpiry() {
        return accessTokenExpiry;
    }

    /**
     * Refresh Token 만료 시간(밀리초)을 반환합니다.
     *
     * @return refreshTokenExpiry
     */
    public long getRefreshTokenExpiry() {
        return refreshTokenExpiry;
    }

}
