package com.dbapplication.bouget.repository;

import com.dbapplication.bouget.entity.RecommendationItem;
import com.dbapplication.bouget.entity.RecommendationSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecommendationItemRepository extends JpaRepository<RecommendationItem, Long> {

    List<RecommendationItem> findBySession(RecommendationSession session);
}
