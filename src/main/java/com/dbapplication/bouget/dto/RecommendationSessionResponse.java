package com.dbapplication.bouget.dto;

import com.dbapplication.bouget.entity.enums.BouquetAtmosphere;
import com.dbapplication.bouget.entity.enums.DressMood;
import com.dbapplication.bouget.entity.enums.DressSilhouette;
import com.dbapplication.bouget.entity.enums.Season;
import com.dbapplication.bouget.entity.enums.WeddingColor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationSessionResponse {

    private Long id;                        // 세션 ID
    private Long userId;                    // 사용자 ID

    private Season season;
    private DressMood dressMood;
    private DressSilhouette dressSilhouette;
    private WeddingColor weddingColor;
    private BouquetAtmosphere bouquetAtmosphere;

    // 이 세션에서 추천된 부케 아이템들 (보통 3개)
    private List<RecommendationItemResponse> items;
}
