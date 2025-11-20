package com.dbapplication.bouget.service;

import com.dbapplication.bouget.dto.UserMeResponse;
import com.dbapplication.bouget.entity.User;
import com.dbapplication.bouget.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserMeResponse getMyInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // profileImageUrl은 DB에 없으니 일단 null로 두고,
        // 컨트롤러에서 세션에 저장된 값으로 채워준다.
        return new UserMeResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                null
        );
    }
}
