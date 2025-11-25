package com.dbapplication.bouget.dto;

import com.dbapplication.bouget.entity.enums.BouquetAtmosphere;
import com.dbapplication.bouget.entity.enums.DressMood;
import com.dbapplication.bouget.entity.enums.DressSilhouette;
import com.dbapplication.bouget.entity.enums.Season;
import com.dbapplication.bouget.entity.enums.WeddingColor;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RecommendationSessionRequest {

    @NotNull
    private Season season;

    @NotNull
    private DressMood dressMood;

    @NotNull
    private DressSilhouette dressSilhouette;

    @NotNull
    private WeddingColor weddingColor;

    @NotNull
    private BouquetAtmosphere bouquetAtmosphere;
}
