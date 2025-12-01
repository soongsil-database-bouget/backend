package com.dbapplication.bouget.service;

import com.dbapplication.bouget.entity.User;
import com.dbapplication.bouget.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    public String getCurrentToken() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new RuntimeException("요청 컨텍스트가 없습니다.");
        }
        HttpServletRequest request = attrs.getRequest();
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Authorization 헤더가 없습니다.");
        }
        return authHeader.substring(7);
    }

    public User getCurrentUser() {
        String token = getCurrentToken();
        return userRepository.findByApiToken(token)
                .orElseThrow(() -> new RuntimeException("유효하지 않은 토큰입니다."));
    }
}
