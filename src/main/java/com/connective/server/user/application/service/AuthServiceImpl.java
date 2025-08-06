package com.connective.server.user.application.service;

import com.connective.server.user.application.client.GoogleAuthClient;
import com.connective.server.user.domain.dto.auth.GoogleTokenResponseDTO;
import com.connective.server.user.domain.dto.auth.GoogleUserInfoResponseDTO;
import com.connective.server.user.domain.dto.auth.LoginResponseDTO;
import com.connective.server.user.domain.dto.auth.TokenDTO;
import com.connective.server.user.domain.entity.User;
import com.connective.server.user.domain.enums.ProfileCharacterType;
import com.connective.server.user.domain.enums.SocialProviderType;
import com.connective.server.user.domain.repository.UserRepository;
import com.connective.server.user.infrastructure.security.JwtCookieUtil;
import com.connective.server.user.infrastructure.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final GoogleAuthClient googleAuthClient;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtCookieUtil jwtCookieUtil;
    private final RedisService redisService;

    /**
     * 1. 인가 코드를 사용하여 구글에 엑세스 토큰 요청 및 발급 (HTTP) 2. 발급받은 엑세스 토큰으로 구글 API를 통해 사용자 정보를 조회 3. 조회한 사용자
     * 정보를 바탕으로 db에 사용자 정보를 저장하고 업데이트 4. 서비스 자체 JWT 발급 및 반환
     */

    @Override
    @Transactional
    public LoginResponseDTO handleGoogleLogin(String code, HttpServletResponse response) {
        // 1. 인가 코드를 사용하여 구글에 엑세스 토큰 요청 및 발급
        GoogleTokenResponseDTO tokenResponse = googleAuthClient.requestAccessToken(code);

        String accessToken = tokenResponse.getAccessToken();
        String refreshToken = tokenResponse.getRefreshToken();
        log.info("Google Access Token (partial): " + accessToken.substring(0,
            Math.min(accessToken.length(), 20)) + "...");

        // 2. 발급받은 엑세스 토큰으로 구글 API를 통해 사용자 정보 조회
        GoogleUserInfoResponseDTO userInfo = googleAuthClient.requestUserInfo(accessToken);
        log.info("Google User Info: Email=" + userInfo.getEmail() + ", Name=" + userInfo.getName());

        // 3. 조회한 사용자 정보를 바탕으로 데이터베이스에 사용자 정보를 저장 및 업데이트
        // social_provider('GOOGLE')와 social_id를 사용하여 DB에 해당 유저가 있는지 조회
        User user = userRepository.findBySocialProviderAndSocialId(SocialProviderType.GOOGLE,
                userInfo.getId())
            .orElseGet(() -> {
                // Case2. (social_provider, social_id) 조합이 DB에 존재하지 않음 (신규 회원)
                log.info("New Google user detected. Registering: " + userInfo.getEmail());

                // User 엔티티의 builder사용해서 객체 생성
                User newUser = User.builder()
                    .email(userInfo.getEmail())
                    .nickname(userInfo.getName())
                    .profileCharacterType(ProfileCharacterType.RABBIT) // 기본캐릭터
                    .statusMessage("hihi")
                    .socialProvider(SocialProviderType.GOOGLE)
                    .socialId(userInfo.getId())
                    .build();
                return userRepository.save(newUser);
            });

        // Case1. 이미 존재하는 유저라면
        boolean updated = false;
        if (!user.getEmail().equals(userInfo.getEmail())) {
            user.updateEmail(userInfo.getEmail());
            updated = true;
        }
        if (user.getNickname() == null || !user.getNickname().equals(userInfo.getName())) {
            user.updateNickname(userInfo.getName());
            updated = true;
        }

        if (user.getProfileCharacterType() == null) {
            user.updateProfileCharacterType(ProfileCharacterType.RABBIT);
            updated = true;
        }

        if (updated) {
            userRepository.save(user);
        }

        log.info("Google login successful and user info processed. User ID: {}, Email: {}",
            user.getId(), user.getEmail());

        // 4. 서비스 자체 JWT 발급 및 반환
        String serviceAccessToken = jwtTokenProvider.generateAccessToken(user);
        String serviceRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        jwtCookieUtil.addRefreshTokenCookie(response, serviceRefreshToken);

        return LoginResponseDTO.builder()
            .accessToken(serviceAccessToken)
            .tokenType("Bearer")
            .expiresIn(jwtTokenProvider.getAccessTokenExpiry() / 1000)
            .build();
    }

    @Override
    public TokenDTO reissueTokens(HttpServletRequest request, HttpServletResponse response) {
        // 1. HTTP Only 쿠키에서 Refresh Token 추출
        String refreshTokenFromCookie = jwtCookieUtil.getRefreshTokenFromCookie(request);
        log.debug("Extracted Refresh Token from cookie: {}", refreshTokenFromCookie != null ?
            refreshTokenFromCookie.substring(0, Math.min(refreshTokenFromCookie.length(), 20))
                + "..." : "null");

        if (refreshTokenFromCookie == null) {
            log.warn("ReissueTokens failed: Refresh Token not found in cookie.");
            throw new RuntimeException("Refresh Token not found.");
        }

        // 2. Refresh Token 유효성 검증
        // 유효하지 않으면 validateToken에서 false 반환
        if (!jwtTokenProvider.validateToken(refreshTokenFromCookie)) {
            log.warn("ReissueTokens failed: Refresh Token invalid.");
            throw new RuntimeException("Refresh Token invalid.");
        }

        // 3. RefreshToken에서 사용자 ID 추출
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshTokenFromCookie);
        if (userId == null) {
            log.warn(
                "ReissueTokens failed: User ID not found in Refresh Token claims or token is malformed.");
            throw new RuntimeException("User ID not found in token."); // 400 Bad Request
        }
        log.debug("User ID extracted from Refresh Token: {}", userId);

        // 4. Redis에서 해당 userId를 키로 저장된 Refresh Token 조회
        String storedRefreshToken = redisService.getValue(String.valueOf(userId));
        log.debug("Stored Refresh Token in Redis for user {}: {}", userId,
            storedRefreshToken != null ?
                storedRefreshToken.substring(0, Math.min(storedRefreshToken.length(), 20)) + "..."
                : "null");

        // 5. 조회된 Refresh Token이 Redis에 존재하지 않거나 요청된 토큰과 일치하지 않는 경우
        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshTokenFromCookie)) {

        }

        // 6. Redis에서 기존 token 삭제
        redisService.deleteValue(String.valueOf(userId));

        // 7. 새로운 token 생성
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));

        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(
            user.getId()); // Refresh Token도 새로 발급

        // 8. 새로운 Refresh Token을 Redis에 저장
        Duration newRefreshTokenDuration = Duration.ofMillis(
            jwtTokenProvider.getRefreshTokenExpiry());
        redisService.setValues(String.valueOf(user.getId()), newRefreshToken,
            newRefreshTokenDuration);
        log.info("New Refresh Token saved to Redis for user {}: {}", userId,
            newRefreshToken.substring(0, Math.min(newRefreshToken.length(), 20)) + "...");

        // 9. 새로운 Refresh Token을 HTTP Only 쿠키로 설정
        jwtCookieUtil.addRefreshTokenCookie(response, newRefreshToken);

        // 10. 클라이언트에 새로운 Access Token 및 Refresh Token 정보 반환 (TokenDTO 사용)
        return TokenDTO.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken) // TokenDTO에는 Refresh Token도 포함하여 반환
            .build();
    }

    /**
     * 사용자 로그아웃 처리: Redis에서 Refresh Token을 삭제하고 클라이언트의 Refresh Token 쿠키를 삭제합니다.
     *
     * @param request  HttpServletRequest (Refresh Token 쿠키 추출용)
     * @param response HttpServletResponse (Refresh Token 쿠키 삭제용)
     */

    @Override
    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        // 1. Refresh Token 쿠키에서 추출
        String refreshToken = jwtCookieUtil.getRefreshTokenFromCookie(request);

        if (refreshToken != null) {
            // 2. Refresh Token에서 userId 추출
            Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
            if (userId != null) {
                // 3. Redis에서 해당 userId로 저장된 Refresh Token 삭제
                redisService.deleteValue(String.valueOf(userId));
                log.info("Logout: Refresh Token deleted from Redis for user {}.", userId);
            } else {
                log.warn(
                    "Logout: Could not extract User ID from Refresh Token. Proceeding with cookie deletion.");
            }
        } else {
            log.warn("Logout: Refresh Token not found in cookie.");
        }
    }
}
