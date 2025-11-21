package com.dbapplication.bouget.controller;

import com.dbapplication.bouget.dto.ApplyImageResponse;
import com.dbapplication.bouget.service.ApplyImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/apply-images")
@Tag(name = "ApplyImage", description = "이미지 합성(적용) 관련 API")
public class ApplyImageController {

    private final ApplyImageService applyImageService;

    /**
     * 이미지 적용 생성
     * 요청 형식: multipart/form-data
     *  - userId      : Long
     *  - bouquetId   : Long
     *  - sessionId   : Long (optional)
     *  - userImage   : file
     */
    @Operation(
            summary = "이미지 적용 생성",
            description = "유저 이미지와 부케 이미지를 합성하여 결과 이미지를 생성하고, ApplyImage로 저장합니다."
    )
    @PostMapping
    public ResponseEntity<ApplyImageResponse> createApplyImage(
            @RequestParam("userId") Long userId,
            @RequestParam("bouquetId") Long bouquetId,
            @RequestParam(value = "sessionId", required = false) Long sessionId,
            @RequestPart("userImage") MultipartFile userImage
    ) {
        ApplyImageResponse response = applyImageService.createApplyImage(
                userId,
                bouquetId,
                sessionId,
                userImage
        );
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "적용 이미지 단건 조회",
            description = "저장된 ApplyImage 한 건을 조회합니다."
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApplyImageResponse> getApplyImage(@PathVariable Long id) {
        ApplyImageResponse response = applyImageService.getApplyImage(id);
        return ResponseEntity.ok(response);
    }

    // 필요하면 나중에 리스트 조회(유저별, 세션별 등)도 추가 가능
}
