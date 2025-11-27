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

    private BouquetCategoryResponse categories;

    private StoreResponse store;
}
