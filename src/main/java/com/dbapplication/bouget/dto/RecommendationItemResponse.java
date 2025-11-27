package com.dbapplication.bouget.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationItemResponse {

    private Long id;        // recommendation_items.id
    private Long bouquetId; // 부케 ID (검색/추가조회용으로 유지)

    // 부케 전체 정보 (카테고리 포함)
    private BouquetResponse bouquet;
}
