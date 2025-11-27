package com.dbapplication.bouget.service;

import com.dbapplication.bouget.dto.InquiryCreateRequest;
import com.dbapplication.bouget.dto.InquiryResponse;
import com.dbapplication.bouget.entity.Inquiry;
import com.dbapplication.bouget.entity.User;
import com.dbapplication.bouget.entity.enums.InquiryStatus;
import com.dbapplication.bouget.repository.InquiryRepository;
import com.dbapplication.bouget.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;

    /**
     * 문의 등록
     */
    @Transactional
    public InquiryResponse createInquiry(Long userId, InquiryCreateRequest request) {
        if (userId == null) {
            // 컨트롤러의 TODO: 401 처리와 연결될 부분
            throw new IllegalStateException("로그인이 필요합니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found. id=" + userId));

        Inquiry inquiry = Inquiry.builder()
                .user(user)
                .content(request.getContent())
                .status(InquiryStatus.NEW)  // 기본값이지만 명시해둠
                .build();

        Inquiry saved = inquiryRepository.save(inquiry);
        return toResponse(saved);
    }

    /**
     * 내 문의 리스트 조회 (페이지네이션)
     */
    @Transactional(readOnly = true)
    public Page<InquiryResponse> getMyInquiries(Long userId, Pageable pageable) {
        if (userId == null) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found. id=" + userId));

        Page<Inquiry> page = inquiryRepository.findByUser(user, pageable);
        return page.map(this::toResponse);
    }

    // === 내부 매핑 메서드 ===

    private InquiryResponse toResponse(Inquiry inquiry) {
        return InquiryResponse.builder()
                .id(inquiry.getId())
                .userId(inquiry.getUser() != null ? inquiry.getUser().getId() : null)
                .content(inquiry.getContent())
                .status(inquiry.getStatus())
                .createdAt(inquiry.getCreatedAt())
                .build();
    }
}
