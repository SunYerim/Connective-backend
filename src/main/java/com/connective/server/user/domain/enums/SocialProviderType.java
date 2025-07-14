package com.connective.server.user.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SocialProviderType {
    GOOGLE("GOOGLE"),
    KAKAO("KAKAO");

    private final String providerName;
}
