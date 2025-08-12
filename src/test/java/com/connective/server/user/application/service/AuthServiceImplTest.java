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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AuthServiceImplTest {

    @Mock
    private GoogleAuthClient googleAuthClient;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private JwtCookieUtil jwtCookieUtil;

    @Mock
    private RedisService redisService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private AuthServiceImpl authService;

    private GoogleTokenResponseDTO googleTokenResponse;
    private GoogleUserInfoResponseDTO googleUserInfo;
    private User testUser;

    @BeforeEach
    void setUp() {
        googleTokenResponse = new GoogleTokenResponseDTO();
        googleTokenResponse.setAccessToken("google-access-token");
        googleTokenResponse.setRefreshToken("google-refresh-token");

        googleUserInfo = new GoogleUserInfoResponseDTO();
        googleUserInfo.setId("google-user-id");
        googleUserInfo.setEmail("test@gmail.com");
        googleUserInfo.setName("Test User");

        testUser = User.builder()
            .email("test@gmail.com")
            .nickname("Test User")
            .profileCharacterType(ProfileCharacterType.RABBIT)
            .statusMessage("hihi")
            .socialProvider(SocialProviderType.GOOGLE)
            .socialId("google-user-id")
            .build();
    }

    @Test
    @DisplayName("구글 로그인 - 신규 사용자 회원가입")
    void handleGoogleLogin_NewUser() {
        // given
        String code = "auth-code";
        when(googleAuthClient.requestAccessToken(code)).thenReturn(googleTokenResponse);
        when(googleAuthClient.requestUserInfo("google-access-token")).thenReturn(googleUserInfo);
        when(userRepository.findBySocialProviderAndSocialId(SocialProviderType.GOOGLE, "google-user-id"))
            .thenReturn(Optional.empty());
        User savedUser = User.builder()
            .email("test@gmail.com")
            .nickname("Test User")
            .profileCharacterType(ProfileCharacterType.RABBIT)
            .statusMessage("hihi")
            .socialProvider(SocialProviderType.GOOGLE)
            .socialId("google-user-id")
            .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenProvider.generateAccessToken(any(User.class))).thenReturn("service-access-token");
        when(jwtTokenProvider.generateRefreshToken(any(Long.class))).thenReturn("service-refresh-token");
        when(jwtTokenProvider.getAccessTokenExpiry()).thenReturn(3600000L);

        // when
        LoginResponseDTO result = authService.handleGoogleLogin(code, response);

        // then
        assertThat(result.getAccessToken()).isEqualTo("service-access-token");
        assertThat(result.getTokenType()).isEqualTo("Bearer");
        assertThat(result.getExpiresIn()).isEqualTo(3600L);

        verify(userRepository).save(any(User.class));
        verify(jwtCookieUtil).addRefreshTokenCookie(response, "service-refresh-token");
    }

    @Test
    @DisplayName("구글 로그인 - 기존 사용자 정보 업데이트")
    void handleGoogleLogin_ExistingUser_UpdateInfo() {
        // given
        String code = "auth-code";
        User existingUser = User.builder()
            .email("old@gmail.com")
            .nickname("Old Name")
            .profileCharacterType(ProfileCharacterType.RABBIT)
            .socialProvider(SocialProviderType.GOOGLE)
            .socialId("google-user-id")
            .build();

        when(googleAuthClient.requestAccessToken(code)).thenReturn(googleTokenResponse);
        when(googleAuthClient.requestUserInfo("google-access-token")).thenReturn(googleUserInfo);
        when(userRepository.findBySocialProviderAndSocialId(SocialProviderType.GOOGLE, "google-user-id"))
            .thenReturn(Optional.of(existingUser));
        when(jwtTokenProvider.generateAccessToken(any(User.class))).thenReturn("service-access-token");
        when(jwtTokenProvider.generateRefreshToken(any(Long.class))).thenReturn("service-refresh-token");
        when(jwtTokenProvider.getAccessTokenExpiry()).thenReturn(3600000L);

        // when
        LoginResponseDTO result = authService.handleGoogleLogin(code, response);

        // then
        assertThat(result.getAccessToken()).isEqualTo("service-access-token");
        verify(userRepository).save(existingUser);
    }

    @Test
    @DisplayName("토큰 재발급 - 성공")
    void reissueTokens_Success() {
        // given
        Long userId = 1L;
        String refreshToken = "refresh-token";
        String newAccessToken = "new-access-token";
        String newRefreshToken = "new-refresh-token";

        when(jwtCookieUtil.getRefreshTokenFromCookie(request)).thenReturn(refreshToken);
        when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(refreshToken)).thenReturn(userId);
        when(redisService.getValue(String.valueOf(userId))).thenReturn(refreshToken);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateAccessToken(testUser)).thenReturn(newAccessToken);
        when(jwtTokenProvider.generateRefreshToken(userId)).thenReturn(newRefreshToken);
        when(jwtTokenProvider.getRefreshTokenExpiry()).thenReturn(604800000L);
        doNothing().when(redisService).deleteValue(anyString());
        doNothing().when(redisService).setValues(anyString(), anyString(), any(Duration.class));
        doNothing().when(jwtCookieUtil).addRefreshTokenCookie(any(HttpServletResponse.class), anyString());

        // when
        TokenDTO result = authService.reissueTokens(request, response);

        // then
        assertThat(result.getAccessToken()).isEqualTo(newAccessToken);
        assertThat(result.getRefreshToken()).isEqualTo(newRefreshToken);

        verify(redisService).deleteValue(String.valueOf(userId));
        verify(redisService).setValues(eq(String.valueOf(userId)), eq(newRefreshToken), any(Duration.class));
        verify(jwtCookieUtil).addRefreshTokenCookie(response, newRefreshToken);
    }

    @Test
    @DisplayName("토큰 재발급 - 쿠키에서 토큰 없음")
    void reissueTokens_NoTokenInCookie() {
        // given
        when(jwtCookieUtil.getRefreshTokenFromCookie(request)).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> authService.reissueTokens(request, response))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Refresh Token not found.");
    }

    @Test
    @DisplayName("토큰 재발급 - 유효하지 않은 토큰")
    void reissueTokens_InvalidToken() {
        // given
        String refreshToken = "invalid-refresh-token";
        when(jwtCookieUtil.getRefreshTokenFromCookie(request)).thenReturn(refreshToken);
        when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.reissueTokens(request, response))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Refresh Token invalid.");
    }

    @Test
    @DisplayName("토큰 재발급 - 토큰에서 사용자 ID 추출 실패")
    void reissueTokens_NoUserIdInToken() {
        // given
        String refreshToken = "refresh-token";
        when(jwtCookieUtil.getRefreshTokenFromCookie(request)).thenReturn(refreshToken);
        when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(refreshToken)).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> authService.reissueTokens(request, response))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("User ID not found in token.");
    }

    @Test
    @DisplayName("토큰 재발급 - Redis에 토큰 없음")
    void reissueTokens_NoTokenInRedis() {
        // given
        Long userId = 1L;
        String refreshToken = "refresh-token";
        when(jwtCookieUtil.getRefreshTokenFromCookie(request)).thenReturn(refreshToken);
        when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(refreshToken)).thenReturn(userId);
        when(redisService.getValue(String.valueOf(userId))).thenReturn(null);

        User mockUser = User.builder()
            .email("test@gmail.com")
            .nickname("Test User")
            .profileCharacterType(ProfileCharacterType.RABBIT)
            .statusMessage("hihi")
            .socialProvider(SocialProviderType.GOOGLE)
            .socialId("google-user-id")
            .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(jwtTokenProvider.generateAccessToken(any(User.class))).thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken(any(Long.class))).thenReturn("new-refresh-token");
        when(jwtTokenProvider.getRefreshTokenExpiry()).thenReturn(604800000L);
        doNothing().when(redisService).deleteValue(anyString());
        doNothing().when(redisService).setValues(anyString(), anyString(), any(Duration.class));
        doNothing().when(jwtCookieUtil).addRefreshTokenCookie(any(HttpServletResponse.class), anyString());

        // when
        TokenDTO result = authService.reissueTokens(request, response);

        // then
        assertThat(result.getAccessToken()).isEqualTo("new-access-token");
        assertThat(result.getRefreshToken()).isEqualTo("new-refresh-token");
        verify(redisService).deleteValue(String.valueOf(userId));
    }

    @Test
    @DisplayName("토큰 재발급 - 사용자 없음")
    void reissueTokens_UserNotFound() {
        // given
        Long userId = 1L;
        String refreshToken = "refresh-token";
        when(jwtCookieUtil.getRefreshTokenFromCookie(request)).thenReturn(refreshToken);
        when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(refreshToken)).thenReturn(userId);
        when(redisService.getValue(String.valueOf(userId))).thenReturn(refreshToken);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.reissueTokens(request, response))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessage("User not found with ID: " + userId);
    }

    @Test
    @DisplayName("로그아웃 - 성공")
    void logout_Success() {
        // given
        Long userId = 1L;
        String refreshToken = "refresh-token";
        when(jwtCookieUtil.getRefreshTokenFromCookie(request)).thenReturn(refreshToken);
        when(jwtTokenProvider.getUserIdFromToken(refreshToken)).thenReturn(userId);

        // when
        authService.logout(request, response);

        // then
        verify(redisService).deleteValue(String.valueOf(userId));
    }

    @Test
    @DisplayName("로그아웃 - 쿠키에서 토큰 없음")
    void logout_NoTokenInCookie() {
        // given
        when(jwtCookieUtil.getRefreshTokenFromCookie(request)).thenReturn(null);

        // when
        authService.logout(request, response);

        // then
        verify(redisService, never()).deleteValue(anyString());
    }

    @Test
    @DisplayName("로그아웃 - 토큰에서 사용자 ID 추출 실패")
    void logout_NoUserIdInToken() {
        // given
        String refreshToken = "refresh-token";
        when(jwtCookieUtil.getRefreshTokenFromCookie(request)).thenReturn(refreshToken);
        when(jwtTokenProvider.getUserIdFromToken(refreshToken)).thenReturn(null);

        // when
        authService.logout(request, response);

        // then
        verify(redisService, never()).deleteValue(anyString());
    }
}