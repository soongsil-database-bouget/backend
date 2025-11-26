package com.dbapplication.bouget.service;

import com.dbapplication.bouget.dto.KakaoLoginRequest;
import com.dbapplication.bouget.dto.KakaoLoginResponse;
import com.dbapplication.bouget.dto.KakaoUserInfo;
import com.dbapplication.bouget.entity.User;
import com.dbapplication.bouget.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class KakaoAuthServiceImpl implements KakaoAuthService {

    private static final String OAUTH_PROVIDER_KAKAO = "KAKAO";

    private final KakaoOAuthClient kakaoOAuthClient;
    private final UserRepository userRepository;

    @Override
    public KakaoLoginResponse login(KakaoLoginRequest request) {
        if (!StringUtils.hasText(request.authorizationCode())
                || !StringUtils.hasText(request.redirectUri())) {
            throw new IllegalArgumentException("authorizationCode와 redirectUri는 필수입니다.");
        }

        // 1) 카카오 액세스토큰 발급
        String kakaoAccessToken =
                kakaoOAuthClient.getAccessToken(request.authorizationCode(), request.redirectUri());

        // 2) 카카오 유저 정보 조회
        KakaoUserInfo kakaoUserInfo = kakaoOAuthClient.getUserInfo(kakaoAccessToken);

        if (kakaoUserInfo.getId() == null) {
            throw new IllegalStateException("카카오 사용자 ID를 가져올 수 없습니다.");
        }

        // 3) 우리 서비스 users 테이블 조회/생성
        boolean isNewUser = false;
        User user = userRepository
                .findByOauthProviderAndOauthSub(OAUTH_PROVIDER_KAKAO, kakaoUserInfo.getId())
                .orElse(null);

        if (user == null) {
            // 신규 회원
            user = User.builder()
                    .email(kakaoUserInfo.getEmail())
                    .name(kakaoUserInfo.getNickname())
                    .oauthProvider(OAUTH_PROVIDER_KAKAO)
                    .oauthSub(kakaoUserInfo.getId())
                    .build();
            user = userRepository.save(user);
            isNewUser = true;
        } else {
            // 기존 회원이면 이메일/이름 변경되었을 수 있으니 업데이트(선택사항)
            user.updateProfile(kakaoUserInfo.getNickname(), kakaoUserInfo.getEmail());
        }

        // 4) 응답 DTO 반환 (서비스용 토큰은 없음)
        return new KakaoLoginResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                kakaoUserInfo.getProfileImageUrl(),
                isNewUser
        );
    }
}
