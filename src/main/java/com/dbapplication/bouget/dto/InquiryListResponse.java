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
public class InquiryListResponse {

    private List<InquiryResponse> items;
    private long totalCount;
}
