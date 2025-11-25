package com.dbapplication.bouget.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BouquetDetailResponse {

    private Long id;
    private String name;
    private int price;
    private String reason;
    private String description;
    private String imageUrl;

    // 이 부케에 연결된 카테고리들 (0개 이상)
    private List<BouquetCategoryResponse> categories;

    private StoreResponse store;
}
