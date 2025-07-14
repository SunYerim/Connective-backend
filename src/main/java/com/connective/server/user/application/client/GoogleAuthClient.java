package com.connective.server.user.application.client;

import com.connective.server.user.domain.dto.auth.GoogleTokenResponseDTO;
import com.connective.server.user.domain.dto.auth.GoogleUserInfoResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
public class GoogleAuthClient {

    private final WebClient webClient;

    @Value("${google.client-id}")
    private String googleClientId;

    @Value("${google.client-secret}")
    private String googleClientSecret;

    @Value("${google.redirect-uri}")
    private String googleRedirectUri;

    public GoogleAuthClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * 구글 인가 코드를 사용하여 액세스 토큰을 요청하고 결과를 즉시 반환합니다 (블로킹).
     *
     * @param code 구글로부터 받은 인가 코드
     * @return GoogleTokenResponse
     * @throws RuntimeException 토큰 요청 실패 시 발생
     */

    public GoogleTokenResponseDTO requestAccessToken(String code) {
        String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("code", code);
        formData.add("client_id", googleClientId);
        formData.add("client_secret", googleClientSecret);
        formData.add("redirect_uri", googleRedirectUri);
        formData.add("grant_type", "authorization_code");

        try {
            return webClient.post()
                .uri(GOOGLE_TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(formData)
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), clientResponse ->
                    clientResponse.bodyToMono(String.class)
                        .map(body -> {
                            return new RuntimeException("Google Token API Client Error: " + body);
                        }))
                .onStatus(status -> status.is5xxServerError(), clientResponse ->
                    clientResponse.bodyToMono(String.class)
                        .map(body -> {
                            return new RuntimeException("Google Token API Server Error: " + body);
                        }))
                .bodyToMono(GoogleTokenResponseDTO.class)
                .block(); // 여기서 블로킹하여 Mono의 결과를 동기적으로 가져옵니다.
        } catch (WebClientResponseException e) {
            throw new RuntimeException("Failed to request Google access token: " + e.getMessage(),
                e);
        } catch (Exception e) {
            System.err.println("Error during Google token request: " + e.getMessage());
            throw new RuntimeException("Failed to request Google access token.", e);
        }
    }

    /**
     * 액세스 토큰을 사용하여 구글 사용자 정보를 요청하고 결과를 즉시 반환합니다 (블로킹).
     *
     * @param accessToken 구글로부터 발급받은 액세스 토큰
     * @return GoogleUserInfoResponse
     * @throws RuntimeException 사용자 정보 요청 실패 시 발생
     */

    public GoogleUserInfoResponseDTO requestUserInfo(String accessToken) {
        String GOOGLE_USERINFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";

        try {
            return webClient.get()
                .uri(GOOGLE_USERINFO_URL)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), clientResponse ->
                    clientResponse.bodyToMono(String.class)
                        .map(body -> {
                            return new RuntimeException(
                                "Google UserInfo API Client Error: " + body);
                        }))
                .onStatus(status -> status.is5xxServerError(), clientResponse ->
                    clientResponse.bodyToMono(String.class)
                        .map(body -> {
                            return new RuntimeException(
                                "Google UserInfo API Server Error: " + body);
                        }))
                .bodyToMono(GoogleUserInfoResponseDTO.class)
                .block();
        } catch (WebClientResponseException e) {
            throw new RuntimeException("Failed to request Google user info: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to request Google user info.", e);
        }
    }


}
