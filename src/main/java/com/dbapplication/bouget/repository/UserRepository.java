package com.dbapplication.bouget.repository;

import com.dbapplication.bouget.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 카카오 로그인용 (provider + sub)
    Optional<User> findByOauthProviderAndOauthSub(String oauthProvider, String oauthSub);
    Optional<User> findByApiToken(String apiToken);  // ★ 추가
    // 필요시 사용
    Optional<User> findByEmail(String email);
}
