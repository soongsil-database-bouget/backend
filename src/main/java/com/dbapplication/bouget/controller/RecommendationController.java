package com.dbapplication.bouget.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 부케 추천 세션/아이템 및 추천 히스토리
 */
@RestController
@RequestMapping("/recommendations")
@RequiredArgsConstructor
@Tag(name = "Recommendations", description = "부케 추천 세션/아이템 및 추천 히스토리")
public class RecommendationController {

    private final RecommendationService recommendationService;

    /**
     * POST /recommendations
     * 부케 추천 세션 생성 (알고리즘)
     *
     * 사용자의 입력값(시즌, 드레스 무드/실루엣, 예식 색감, 부케 분위기 등)을 기반으로
     * 추천 세션을 생성하고 관련 아이템(추천된 부케 3개)을 저장/반환합니다.
     */
    @PostMapping
    @Operation(
            summary = "부케 추천 세션 생성 (알고리즘)",
            description = "사용자의 입력값(시즌, 드레스 무드/실루엣, 예식 색감, 부케 분위기 등)을 기반으로 "
                    + "추천 세션을 생성하고 관련 아이템(추천된 부케 3개)을 저장/반환합니다."
    )
    public ResponseEntity<RecommendationSessionResponse> createRecommendationSession(
            @Parameter(description = "추천 세션 생성 요청 옵션")
            @RequestBody @Valid RecommendationSessionRequest request
    ) {
        RecommendationSessionResponse response = recommendationService.createSession(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /recommendations
     * 내 추천 히스토리 조회
     *
     * - page는 1부터 시작하는 값으로 받고, 내부에서 0-based 로 변환
     * - 응답 스펙: { items: [...], totalCount: n }
     */
    @GetMapping
    @Operation(
            summary = "내 추천 히스토리 조회",
            description = "현재 로그인한 사용자가 지금까지 받은 부케 추천 세션 리스트를 조회합니다."
    )
    public ResponseEntity<RecommendationSessionListResponse> getMyRecommendationSessions(
            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            @RequestParam(name = "page", defaultValue = "1") int page,

            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        int safePage = Math.max(page, 1);
        Pageable pageable = PageRequest.of(safePage - 1, size);

        Page<RecommendationSessionResponse> sessionPage = recommendationService.getMySessions(pageable);

        RecommendationSessionListResponse response = RecommendationSessionListResponse.builder()
                .items(sessionPage.getContent())
                .totalCount(sessionPage.getTotalElements())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * GET /recommendations/{sessionId}
     * 추천 세션 상세 조회
     */
    @GetMapping("/{sessionId}")
    @Operation(
            summary = "추천 세션 상세 조회",
            description = "특정 추천 세션의 입력 옵션과 추천된 부케 아이템 전체를 조회합니다."
    )
    public ResponseEntity<RecommendationSessionResponse> getRecommendationSessionDetail(
            @Parameter(description = "추천 세션 ID", required = true, example = "1")
            @PathVariable("sessionId") Long sessionId
    ) {
        RecommendationSessionResponse response = recommendationService.getSessionDetail(sessionId);
        return ResponseEntity.ok(response);
    }
}
