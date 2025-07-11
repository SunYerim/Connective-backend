package com.connective.server.user.application.service;

import com.connective.server.user.application.client.GoogleAuthClient;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final GoogleAuthClient googleAuthClient;

    // TODO: JWT 관련 컴포넌트 주입

    /**
     * 1. 인가 코드를 사용하여 구글에 엑세스 토큰 요청 및 발급 (HTTP)
     * 2. 발급받은 엑세스 토큰으로 구글 API를 통해 사용자 정보를 조회
     * 3. 조회한 사용자 정보를 바탕으로 db에 사용자 정보를 저장하고 업데이트
     * 4. 서비스 자체 JWT 발급 및 반환
     * */

    @Override
    @Transactional
    public String handleGoogleLogin(String code) {



        return "";
    }
}
