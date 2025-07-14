package com.connective.server.user.domain.repository;

import com.connective.server.user.domain.entity.User;
import com.connective.server.user.domain.enums.SocialProviderType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 주어진 소셜 제공자(socialProvider)와 소셜 ID(socialId)를 사용하여 User를 조회합니다. 사용자가 존재하지 않을 경우
     * Optional.empty()를 반환합니다.
     *
     * @param socialProvider 소셜 로그인 제공자 (예: "GOOGLE", "KAKAO")
     * @param socialId       소셜 서비스에서 부여한 사용자 고유 ID
     * @return 일치하는 User 엔티티를 포함하는 Optional, 없으면 Optional.empty()
     */
    Optional<User> findBySocialProviderAndSocialId(SocialProviderType socialProvider, String socialId);

}
