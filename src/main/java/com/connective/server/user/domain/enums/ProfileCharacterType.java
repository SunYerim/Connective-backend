package com.connective.server.user.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProfileCharacterType {
    DOLPHIN("Dolphin"),
    RABBIT("Rabbit"),
    BEAR("Bear");

    private final String displayName;
}
