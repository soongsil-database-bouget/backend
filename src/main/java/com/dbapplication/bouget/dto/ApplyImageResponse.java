package com.dbapplication.bouget.dto;

import com.dbapplication.bouget.entity.enums.ApplyStatus;

import java.time.LocalDateTime;

public record ApplyImageResponse(
        Long id,
        Long userId,
        Long bouquetId,
        Long sessionId,
        String srcImageUrl,
        String genImageUrl,
        ApplyStatus status,
        LocalDateTime createdAt,
        BouquetResponse bouquet
) {}

