package com.dbapplication.bouget.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreResponse {

    private Long id;
    private Long bouquetId;
    private String storeName;
    private String storeUrl;
    private String instagramId;
}
