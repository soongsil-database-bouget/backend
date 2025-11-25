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

    private Long id;                 // recommendation_items.id
    private Long bouquetId;          // 부케 ID
    private String bouquetName;      // 부케 이름
    private int bouquetPrice;        // 부케 가격
    private String bouquetImageUrl;  // 부케 이미지 URL
}
