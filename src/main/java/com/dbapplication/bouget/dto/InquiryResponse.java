package com.dbapplication.bouget.dto;

import com.dbapplication.bouget.entity.enums.InquiryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InquiryResponse {

    private Long id;
    private Long userId;
    private String content;
    private InquiryStatus status;
    private LocalDateTime createdAt;
}
