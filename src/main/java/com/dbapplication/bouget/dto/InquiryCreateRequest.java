package com.dbapplication.bouget.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InquiryCreateRequest {

    @NotBlank(message = "문의 내용을 입력해주세요.")
    private String content;
}
