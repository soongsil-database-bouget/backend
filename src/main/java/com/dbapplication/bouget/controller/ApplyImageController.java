package com.dbapplication.bouget.controller;

import com.dbapplication.bouget.dto.ApplyImageResponse;
import com.dbapplication.bouget.dto.ApplyImageListResponse;
import com.dbapplication.bouget.service.ApplyImageService;
import com.dbapplication.bouget.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/virtual-fittings")
@Tag(name = "VirtualFittings", description = "이미지 합성(가상 피팅) 및 히스토리 관련 API")
public class ApplyImageController {

    private final ApplyImageService applyImageService;
    private final AuthService authService;

    /**
     * 가상 피팅 요청 (이미지 합성)
     * 요청 형식: multipart/form-data
     *  - user_image : file (사용자 원본 이미지)
     *  - bouquet_id : Long (필수)
     *  - session_id : Long (선택, 추천 세션 ID)
     *
     * userId는 Authorization: Bearer <token> 에서 조회.
     */
    @Operation(
            summary = "가상 피팅 요청 (이미지 합성)",
            description = "사용자 원본 이미지와 선택한 부케를 기반으로 합성 이미지를 생성하고, ApplyImage로 저장합니다."
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApplyImageResponse> createApplyImage(
            @Parameter(description = "합성할 부케 ID", required = true, example = "1")
            @RequestParam("bouquet_id") Long bouquetId,

            @Parameter(description = "관련 추천 세션 ID (선택)", required = false, example = "10")
            @RequestParam(value = "session_id", required = false) Long sessionId,

            @Parameter(description = "사용자 원본 이미지 파일", required = true)
            @RequestPart("user_image") MultipartFile userImage
    ) {
        // ★ 세션 대신 토큰에서 현재 사용자 조회
        Long userId = authService.getCurrentUser().getId();

        ApplyImageResponse response = applyImageService.createApplyImage(
                userId,
                bouquetId,
                sessionId,
                userImage
        );
        // 현재 구현은 동기 합성(완료 후 결과 반환) 기준으로 200 OK 사용
        return ResponseEntity.ok(response);
    }
    /**
     * GET /virtual-fittings
     * 내가 적용해본 가상 피팅 리스트 (마이페이지)
     */
    @Operation(
            summary = "내 가상 피팅 히스토리 조회",
            description = "현재 로그인한 사용자가 시도했던 가상 피팅(이미지 합성) 히스토리를 조회합니다."
    )
    @GetMapping
    public ResponseEntity<ApplyImageListResponse> getMyApplyImages(
            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            @RequestParam(name = "page", defaultValue = "1") int page,

            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        Long userId = authService.getCurrentUser().getId();   // ★ 여기서도 토큰 기반

        int safePage = Math.max(page, 1);
        Pageable pageable = PageRequest.of(safePage - 1, size);

        Page<ApplyImageResponse> applyImagePage =
                applyImageService.getApplyImagesByUser(userId, pageable);

        ApplyImageListResponse response = ApplyImageListResponse.builder()
                .items(applyImagePage.getContent())
                .totalCount(applyImagePage.getTotalElements())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * GET /virtual-fittings/{id}
     * 적용 이미지 단건 조회
     */
    @Operation(
            summary = "가상 피팅 단건 조회",
            description = "저장된 ApplyImage 한 건(합성 결과)의 상세 정보를 조회합니다."
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApplyImageResponse> getApplyImage(
            @Parameter(description = "적용 이미지 ID", required = true, example = "1")
            @PathVariable("id") Long id
    ) {
        ApplyImageResponse response = applyImageService.getApplyImage(id);
        return ResponseEntity.ok(response);
    }
}