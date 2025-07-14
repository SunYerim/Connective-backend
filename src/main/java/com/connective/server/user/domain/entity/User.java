package com.connective.server.user.domain.entity;

import com.connective.server.user.domain.enums.ProfileCharacterType;
import com.connective.server.user.domain.enums.SocialProviderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "User")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "nickname")
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_character_type")
    private ProfileCharacterType profileCharacterType;

    @Column(name = "status_message")
    private String statusMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "social_provider", nullable = false)
    private SocialProviderType socialProvider; // KAKAO , GOOGLE

    @Column(name = "social_id", nullable = false, unique = true)
    private String socialId;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public User(String email, String nickname, ProfileCharacterType profileCharacterType,
        String statusMessage,
        SocialProviderType socialProvider, String socialId) {
        this.email = email;
        this.nickname = nickname;
        this.profileCharacterType = profileCharacterType;
        this.statusMessage = statusMessage;
        this.socialProvider = socialProvider;
        this.socialId = socialId;
    }

    // --- service단에서 필요한 메서드
    public void updateEmail(String email) {
        this.email = email;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public void updateProfileCharacterType(ProfileCharacterType profileCharacterType) {
        this.profileCharacterType = profileCharacterType;
    }


}
