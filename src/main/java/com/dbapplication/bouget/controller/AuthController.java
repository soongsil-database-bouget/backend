package com.dbapplication.bouget.controller;

import com.dbapplication.bouget.dto.KakaoLoginRequest;
import com.dbapplication.bouget.dto.KakaoLoginResponse;
import com.dbapplication.bouget.service.KakaoAuthService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final KakaoAuthService kakaoAuthService;

    /**
     * 카카오 로그인 & 회원가입
     * - 인가코드 + redirectUri 필수
     * - 로그인 성공 시 HttpSession에 userId 등 저장
     */
    @PostMapping("/kakao/login")
    public ResponseEntity<KakaoLoginResponse> kakaoLogin(
            @RequestBody KakaoLoginRequest request,
            HttpSession session
    ) {
        if (!StringUtils.hasText(request.authorizationCode())
                || !StringUtils.hasText(request.redirectUri())) {
            throw new IllegalArgumentException("authorizationCode와 redirectUri는 필수입니다.");
        }

        KakaoLoginResponse response = kakaoAuthService.login(request);

        // 세션에 기본 정보 저장 (필요한 것만)
        session.setAttribute("userId", response.userId());
        session.setAttribute("userEmail", response.email());
        session.setAttribute("userName", response.name());
        session.setAttribute("profileImageUrl", response.profileImageUrl());

        return ResponseEntity.ok(response);
    }

    /**
     * 로그아웃
     * - 세션 무효화
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.noContent().build();
    }
}
