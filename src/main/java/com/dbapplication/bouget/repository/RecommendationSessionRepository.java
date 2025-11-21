package com.dbapplication.bouget.repository;

import com.dbapplication.bouget.entity.RecommendationSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecommendationSessionRepository extends JpaRepository<RecommendationSession, Long> {

}
