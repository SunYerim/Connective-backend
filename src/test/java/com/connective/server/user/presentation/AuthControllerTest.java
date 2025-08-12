package com.connective.server.user.presentation;

import com.connective.server.user.application.service.AuthService;
import com.connective.server.user.domain.dto.auth.LoginResponseDTO;
import com.connective.server.user.domain.dto.auth.TokenDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class,
    excludeAutoConfiguration = {SecurityAutoConfiguration.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;


    @Test
    @DisplayName("구글 콜백 - 성공")
    void googleCallback_Success() throws Exception {
        // given
        String code = "test-authorization-code";
        LoginResponseDTO loginResponse = LoginResponseDTO.builder()
            .accessToken("test-access-token")
            .tokenType("Bearer")
            .expiresIn(3600L)
            .build();

        when(authService.handleGoogleLogin(eq(code), any(HttpServletResponse.class)))
            .thenReturn(loginResponse);

        // when & then
        mockMvc.perform(get("/auth/google/callback")
                .param("code", code))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("test-access-token"))
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    @DisplayName("구글 콜백 - 코드 누락")
    void googleCallback_MissingCode() throws Exception {
        // when & then
        mockMvc.perform(get("/auth/google/callback"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("구글 콜백 - 빈 코드")
    void googleCallback_EmptyCode() throws Exception {
        // when & then
        mockMvc.perform(get("/auth/google/callback")
                .param("code", ""))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("구글 콜백 - 서비스 예외")
    void googleCallback_ServiceException() throws Exception {
        // given
        String code = "test-authorization-code";
        when(authService.handleGoogleLogin(eq(code), any(HttpServletResponse.class)))
            .thenThrow(new RuntimeException("Google login failed"));

        // when & then
        mockMvc.perform(get("/auth/google/callback")
                .param("code", code))
            .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("토큰 재발급 - 성공")
    void reissueTokens_Success() throws Exception {
        // given
        TokenDTO tokenDTO = TokenDTO.builder()
            .accessToken("new-access-token")
            .refreshToken("new-refresh-token")
            .build();

        when(authService.reissueTokens(any(HttpServletRequest.class), any(HttpServletResponse.class)))
            .thenReturn(tokenDTO);

        // when & then
        mockMvc.perform(post("/auth/reissue")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("new-access-token"))
            .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
    }

    @Test
    @DisplayName("토큰 재발급 - 실패")
    void reissueTokens_Failure() throws Exception {
        // given
        when(authService.reissueTokens(any(HttpServletRequest.class), any(HttpServletResponse.class)))
            .thenThrow(new RuntimeException("Invalid refresh token"));

        // when & then
        mockMvc.perform(post("/auth/reissue")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("로그아웃 - 성공")
    void logout_Success() throws Exception {
        // given
        doNothing().when(authService).logout(any(HttpServletRequest.class), any(HttpServletResponse.class));

        // when & then
        mockMvc.perform(post("/auth/logout")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string("Logout successful."));
    }

    @Test
    @DisplayName("로그아웃 - 실패")
    void logout_Failure() throws Exception {
        // given
        doThrow(new RuntimeException("Logout failed"))
            .when(authService).logout(any(HttpServletRequest.class), any(HttpServletResponse.class));

        // when & then
        mockMvc.perform(post("/auth/logout")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError())
            .andExpect(content().string("Failed to logout."));
    }
}