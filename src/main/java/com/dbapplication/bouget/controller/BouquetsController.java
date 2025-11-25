package com.dbapplication.bouget.controller;


import com.dbapplication.bouget.entity.enums.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 부케 정보 조회 (전체, 필터, 상세)
 */
@RestController
@RequestMapping("/bouquets")
@RequiredArgsConstructor
@Tag(name = "Bouquets", description = "부케 정보 조회 (전체, 필터, 상세)")
public class BouquetsController {

    private final BouquetQueryService bouquetQueryService;

    /**
     * GET /bouquets
     * 부케 리스트 조회 (전체 / 필터)
     *
     * - 프론트 스펙과 맞추기 위해 쿼리 파라미터 이름은 snake_case 그대로 사용
     * - page는 1부터 시작하는 값으로 받고, 내부적으로는 0-based Pageable 로 변환
     */
    @GetMapping
    @Operation(
            summary = "부케 리스트 조회 (전체 / 필터)",
            description = "전체 부케 리스트를 조회하거나, ERD의 속성(season, dress_mood 등)별로 필터링하여 조회합니다."
    )
    public ResponseEntity<BouquetListResponse> getBouquets(
            @Parameter(description = "필터 - 시즌")
            @RequestParam(name = "season", required = false) Season season,

            @Parameter(description = "필터 - 부케 분위기")
            @RequestParam(name = "bouquet_atmosphere", required = false) BouquetAtmosphere bouquetAtmosphere,

            @Parameter(description = "필터 - 사용 용도")
            @RequestParam(name = "usage", required = false) Usage usage,

            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            @RequestParam(name = "page", defaultValue = "1") int page,

            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        // 방어 로직: page는 최소 1
        int safePage = Math.max(page, 1);
        Pageable pageable = PageRequest.of(safePage - 1, size);

        // Service 레이어에서 필터/페이징 처리
        Page<BouquetResponse> bouquetPage = bouquetQueryService.getBouquets(
                season,
                bouquetAtmosphere,
                usage,
                pageable
        );

        // 응답 스펙: { bouquets: [...], totalCount: n }
        BouquetListResponse response = BouquetListResponse.builder()
                .bouquets(bouquetPage.getContent())
                .totalCount(bouquetPage.getTotalElements())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * GET /bouquets/{bouquetId}
     * 부케 상세 정보 조회
     */
    @GetMapping("/{bouquetId}")
    @Operation(
            summary = "부케 상세 정보 조회",
            description = "특정 부케의 상세 정보(가격, 설명, 이미지, 카테고리/속성, 스토어 등)를 조회합니다."
    )
    public ResponseEntity<BouquetDetailResponse> getBouquetDetail(
            @Parameter(description = "부케 ID", required = true, example = "1")
            @PathVariable("bouquetId") Long bouquetId
    ) {
        BouquetDetailResponse detail = bouquetQueryService.getBouquetDetail(bouquetId);
        return ResponseEntity.ok(detail);
    }
}
