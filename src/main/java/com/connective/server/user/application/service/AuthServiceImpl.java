package com.connective.server.user.application.service;

import com.connective.server.user.application.client.GoogleAuthClient;
import com.connective.server.user.domain.dto.auth.GoogleTokenResponseDTO;
import com.connective.server.user.domain.dto.auth.GoogleUserInfoResponseDTO;
import com.connective.server.user.domain.entity.User;
import com.connective.server.user.domain.enums.ProfileCharacterType;
import com.connective.server.user.domain.enums.SocialProviderType;
import com.connective.server.user.domain.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final GoogleAuthClient googleAuthClient;
    private final UserRepository userRepository;

    // TODO: JWT 관련 컴포넌트 주입

    /**
     * 1. 인가 코드를 사용하여 구글에 엑세스 토큰 요청 및 발급 (HTTP) 2. 발급받은 엑세스 토큰으로 구글 API를 통해 사용자 정보를 조회 3. 조회한 사용자
     * 정보를 바탕으로 db에 사용자 정보를 저장하고 업데이트 4. 서비스 자체 JWT 발급 및 반환
     */

    @Override
    @Transactional
    public String handleGoogleLogin(String code) {
        // 1. 인카 코드를 사용하여 구글에 엑세스 토큰 요청 및 발급
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

        return "Google 로그인 성공 및 사용자 정보 DB 처리 완료! User ID: " + user.getId() + ", Email: "
            + user.getEmail();
    }
}
