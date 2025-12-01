package com.dbapplication.bouget.entity;

import com.dbapplication.bouget.entity.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 255)
    private String email;

    @Column(length = 255)
    private String name;

    @Column(name = "oauth_provider", length = 255)
    private String oauthProvider;

    @Column(name = "oauth_sub", length = 255)
    private String oauthSub;        // 카카오 id 등

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 255, nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "api_token", unique = true)
    private String apiToken;   // ★ 추가

    // 카카오 프로필 업데이트 등에 사용
    public void updateProfile(String name, String email) {
        this.name = name;
        this.email = email;
    }
}
