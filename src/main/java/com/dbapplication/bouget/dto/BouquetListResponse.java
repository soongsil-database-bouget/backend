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
public class BouquetListResponse {

    // 부케 목록
    private List<BouquetResponse> bouquets;

    // 전체 개수 (페이징 정보용)
    private long totalCount;
}
