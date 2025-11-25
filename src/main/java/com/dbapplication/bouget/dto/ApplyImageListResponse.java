package com.dbapplication.bouget.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ApplyImageListResponse {
    private final List<ApplyImageResponse> items;
    private final long totalCount;
}
