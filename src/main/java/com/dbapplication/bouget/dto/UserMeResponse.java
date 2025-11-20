package com.dbapplication.bouget.dto;

public record UserMeResponse(
        Long id,
        String email,
        String name,
        String profileImageUrl
) {
}
