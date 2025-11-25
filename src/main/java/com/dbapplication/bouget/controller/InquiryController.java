package com.dbapplication.bouget.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/inquiries")
@Tag(name = "Inquiries", description = "문의 관련 API")
public class InquiryController {

    private final InquiryService inquiryService;

    /**
     * POST /inquiries
     * 문의 등록
     */
    @PostMapping
    @Operation(
            summary = "문의 등록",
            description = "현재 로그인한 사용자의 문의를 등록합니다."
    )
    public ResponseEntity<InquiryResponse> createInquiry(
            HttpSession session,
            @RequestBody @Valid InquiryCreateRequest request
    ) {
        Long userId = (Long) session.getAttribute("userId");
        // TODO: userId가 null이면 401 Unauthorized 처리 (전역 예외 처리에서)

        InquiryResponse response = inquiryService.createInquiry(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /inquiries/my
     * 내 문의 리스트 조회
     *
     * 응답 형식:
     * {
     *   "items": [ InquiryResponse... ],
     *   "totalCount": 123
     * }
     */
    @GetMapping("/my")
    @Operation(
            summary = "내 문의 리스트 조회",
            description = "현재 로그인한 사용자가 작성한 문의 목록을 조회합니다."
    )
    public ResponseEntity<InquiryListResponse> getMyInquiries(
            HttpSession session,

            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            @RequestParam(name = "page", defaultValue = "1") int page,

            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        Long userId = (Long) session.getAttribute("userId");
        // TODO: userId null 시 401 처리

        int safePage = Math.max(page, 1);
        Pageable pageable = PageRequest.of(safePage - 1, size);

        Page<InquiryResponse> inquiryPage = inquiryService.getMyInquiries(userId, pageable);

        InquiryListResponse response = InquiryListResponse.builder()
                .items(inquiryPage.getContent())
                .totalCount(inquiryPage.getTotalElements())
                .build();

        return ResponseEntity.ok(response);
    }
}
