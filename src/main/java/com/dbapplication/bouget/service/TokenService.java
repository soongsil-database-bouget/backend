package com.dbapplication.bouget.service;

import com.dbapplication.bouget.entity.User;
import com.dbapplication.bouget.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final UserRepository userRepository;

    public String issueTokenForUser(User user) {
        String token = UUID.randomUUID().toString(); // 간단 랜덤 토큰
        user.setApiToken(token);
        userRepository.save(user);
        return token;
    }
    public void revokeToken(String token) {
        userRepository.findByApiToken(token).ifPresent(user -> {
            user.setApiToken(null);
            userRepository.save(user);
        });
    }
}
