package com.dbapplication.bouget.dto;

public record KakaoLoginResponse(
        Long userId,
        String email,
        String name,
        String profileImageUrl,

        boolean isNewUser
) {
}
