package com.dbapplication.bouget.controller;

import com.dbapplication.bouget.dto.KakaoLoginRequest;
import com.dbapplication.bouget.dto.KakaoLoginResponse;
import com.dbapplication.bouget.service.AuthService;
import com.dbapplication.bouget.service.KakaoAuthService;
import com.dbapplication.bouget.service.TokenService;
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
    private final AuthService authService;
    private final TokenService tokenService;

    /**
     * 카카오 로그인 & 회원가입
     * - 인가코드 + redirectUri 필수
     * - 로그인 성공 시 HttpSession에 userId 등 저장
     */
    @PostMapping("/kakao/login")
    public ResponseEntity<KakaoLoginResponse> kakaoLogin(
            @RequestBody KakaoLoginRequest request
    ) {
        if (!StringUtils.hasText(request.authorizationCode())
                || !StringUtils.hasText(request.redirectUri())) {
            throw new IllegalArgumentException("authorizationCode와 redirectUri는 필수입니다.");
        }
        KakaoLoginResponse response = kakaoAuthService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 로그아웃
     * **/
     @PostMapping("/logout")
     public ResponseEntity<Void> logout() {
     String token = authService.getCurrentToken(); // 토큰 없으면 401 던짐
     tokenService.revokeToken(token);             // DB에서 api_token 제거
     return ResponseEntity.noContent().build();   // 204
     }
}
