package com.dbapplication.bouget.dto;

import com.dbapplication.bouget.entity.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BouquetCategoryResponse {

    private Season season;
    private DressMood dressMood;
    private DressSilhouette dressSilhouette;
    private WeddingColor weddingColor;
    private BouquetAtmosphere bouquetAtmosphere;
    private Usage usage;
}
