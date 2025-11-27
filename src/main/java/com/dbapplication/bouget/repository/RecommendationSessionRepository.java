package com.dbapplication.bouget.repository;

import com.dbapplication.bouget.entity.RecommendationSession;
import com.dbapplication.bouget.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecommendationSessionRepository extends JpaRepository<RecommendationSession, Long> {
    Page<RecommendationSession> findByUser(User user, Pageable pageable);
}
