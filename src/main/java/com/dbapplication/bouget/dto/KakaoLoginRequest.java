package com.dbapplication.bouget.dto;

/**
 * 카카오 로그인 요청 DTO
 * - authorizationCode + redirectUri
 */
public record KakaoLoginRequest(
        String authorizationCode,
        String redirectUri
) {
}
