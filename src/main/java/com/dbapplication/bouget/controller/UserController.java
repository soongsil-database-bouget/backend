package com.dbapplication.bouget.controller;

import com.dbapplication.bouget.dto.UserMeResponse;
import com.dbapplication.bouget.service.AuthService;
import com.dbapplication.bouget.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    @GetMapping("/me")
    public ResponseEntity<UserMeResponse> getMe() {
        Long userId = authService.getCurrentUser().getId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        UserMeResponse base = userService.getMyInfo(userId);

        UserMeResponse response = new UserMeResponse(
                base.id(),
                base.email(),
                base.name(),
                base.profileImageUrl()
        );

        return ResponseEntity.ok(response);
    }
}
