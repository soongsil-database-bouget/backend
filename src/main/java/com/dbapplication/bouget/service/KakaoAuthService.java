package com.dbapplication.bouget.service;

import com.dbapplication.bouget.dto.KakaoLoginRequest;
import com.dbapplication.bouget.dto.KakaoLoginResponse;

public interface KakaoAuthService {

    /**
     * 카카오 로그인 / 회원가입 플로우
     * 1) authorizationCode + redirectUri로 카카오 액세스토큰 발급
     * 2) 액세스토큰으로 카카오 유저 정보 조회
     * 3) users 테이블에서 조회 or 신규 생성
     * 4) 유저 정보 + isNewUser 반환
     */
    KakaoLoginResponse login(KakaoLoginRequest request);
}
