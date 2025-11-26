package com.dbapplication.bouget.service;

import com.dbapplication.bouget.dto.KakaoUserInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoOAuthClient {

    @Value("${kakao.oauth.client-id}")
    private String clientId;

    @Value("${kakao.oauth.client-secret:}")
    private String clientSecret;

    @Value("${kakao.oauth.token-uri:https://kauth.kakao.com/oauth/token}")
    private String tokenUri;

    @Value("${kakao.oauth.user-info-uri:https://kapi.kakao.com/v2/user/me}")
    private String userInfoUri;

    // 간단하게 인스턴스 생성 (Bean으로 따로 빼고 싶으면 @Bean 등록해도 됨)
    private RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    /**
     * 인가코드 + redirectUri로 카카오 액세스토큰 발급
     */

    public String getAccessToken(String authorizationCode, String redirectUri) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        if (clientSecret != null && !clientSecret.isBlank()) {
            params.add("client_secret", clientSecret);
        }
        params.add("redirect_uri", redirectUri);
        params.add("code", authorizationCode);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> entity =
                new HttpEntity<>(params, headers);

        try {
            ResponseEntity<String> response =
                    restTemplate.postForEntity(tokenUri, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("[KakaoToken] status={}, body={}",
                        response.getStatusCodeValue(), response.getBody());
                throw new IllegalStateException("카카오 토큰 요청 실패");
            }

            KakaoTokenResponse body =
                    objectMapper.readValue(response.getBody(), KakaoTokenResponse.class);

            if (body.getAccessToken() == null) {
                log.error("[KakaoToken] no access_token in body={}", response.getBody());
                throw new IllegalStateException("카카오 토큰 발급 실패: access_token 없음");
            }

            return body.getAccessToken();

        } catch (HttpStatusCodeException e) {
            // ★ 카카오가 400/401/500 보낼 때 여기로 옴
            log.error("[KakaoToken] HTTP error: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalStateException("카카오 토큰 요청 중 오류 발생", e);
        } catch (Exception e) {
            log.error("[KakaoToken] Unexpected error", e);
            throw new IllegalStateException("카카오 토큰 처리 중 예기치 못한 오류", e);
        }
    }
    /**
     * 카카오 액세스토큰으로 유저 정보 조회
     */
    public KakaoUserInfo getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<KakaoUserInfoResponse> response =
                restTemplate.exchange(userInfoUri, HttpMethod.GET, entity, KakaoUserInfoResponse.class);

        KakaoUserInfoResponse body = response.getBody();
        if (body == null) {
            throw new RuntimeException("카카오 사용자 정보 조회에 실패했습니다.");
        }

        return body.toUserInfo();
    }

    // ---------- 내부 응답 DTO ----------

    @Data
    public static class KakaoTokenResponse {
        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("token_type")
        private String tokenType;

        @JsonProperty("refresh_token")
        private String refreshToken;

        @JsonProperty("expires_in")
        private Long expiresIn;
    }

    @Data
    public static class KakaoUserInfoResponse {
        private Long id;

        @JsonProperty("kakao_account")
        private KakaoAccount kakaoAccount;

        @Data
        public static class KakaoAccount {
            private String email;
            private Profile profile;

            @Data
            public static class Profile {
                private String nickname;

                @JsonProperty("profile_image_url")
                private String profileImageUrl;
            }
        }

        public KakaoUserInfo toUserInfo() {
            KakaoUserInfo info = new KakaoUserInfo();
            info.setId(id != null ? String.valueOf(id) : null);

            if (kakaoAccount != null) {
                info.setEmail(kakaoAccount.getEmail());
                if (kakaoAccount.getProfile() != null) {
                    info.setNickname(kakaoAccount.getProfile().getNickname());
                    info.setProfileImageUrl(kakaoAccount.getProfile().getProfileImageUrl());
                }
            }
            return info;
        }
    }
}
