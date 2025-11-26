package com.dbapplication.bouget.service;

import com.dbapplication.bouget.dto.BouquetCategoryResponse;
import com.dbapplication.bouget.dto.BouquetResponse;
import com.dbapplication.bouget.dto.RecommendationItemResponse;
import com.dbapplication.bouget.dto.RecommendationSessionRequest;
import com.dbapplication.bouget.dto.RecommendationSessionResponse;
import com.dbapplication.bouget.entity.Bouquet;
import com.dbapplication.bouget.entity.BouquetCategory;
import com.dbapplication.bouget.entity.RecommendationItem;
import com.dbapplication.bouget.entity.RecommendationSession;
import com.dbapplication.bouget.entity.User;
import com.dbapplication.bouget.repository.BouquetCategoryRepository;
import com.dbapplication.bouget.repository.BouquetRepository;
import com.dbapplication.bouget.repository.RecommendationItemRepository;
import com.dbapplication.bouget.repository.RecommendationSessionRepository;
import com.dbapplication.bouget.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final RecommendationSessionRepository sessionRepository;
    private final RecommendationItemRepository itemRepository;
    private final BouquetRepository bouquetRepository;
    private final UserRepository userRepository;
    private final BouquetCategoryRepository bouquetCategoryRepository; // ✅ 추가

    /**
     * 추천 세션 생성 + 추천 아이템 3개 저장
     */
    @Transactional
    public RecommendationSessionResponse createSession(RecommendationSessionRequest request) {
        User user = getCurrentUser();

        // 1. 추천 세션 생성
        RecommendationSession session = RecommendationSession.builder()
                .user(user)
                .season(request.getSeason())
                .dressMood(request.getDressMood())
                .dressSilhouette(request.getDressSilhouette())
                .weddingColor(request.getWeddingColor())
                .bouquetAtmosphere(request.getBouquetAtmosphere())
                .build();

        RecommendationSession savedSession = sessionRepository.save(session);

        // 2. 추천 알고리즘으로 부케 3개 선정
        List<Bouquet> recommendedBouquets = pickRecommendedBouquets(savedSession);

        // 3. RecommendationItem 저장
        List<RecommendationItem> itemsToSave = new ArrayList<>();
        for (Bouquet bouquet : recommendedBouquets) {
            RecommendationItem item = RecommendationItem.builder()
                    .session(savedSession)
                    .bouquet(bouquet)
                    .build();
            itemsToSave.add(item);
        }

        List<RecommendationItem> savedItems = itemRepository.saveAll(itemsToSave);

        // 4. 응답 DTO로 변환
        List<RecommendationItemResponse> itemResponses = savedItems.stream()
                .map(this::toItemResponse)
                .toList();

        return toSessionResponse(savedSession, itemResponses);
    }

    /**
     * 내 추천 세션 히스토리 조회
     */
    @Transactional(readOnly = true)
    public Page<RecommendationSessionResponse> getMySessions(Pageable pageable) {
        User user = getCurrentUser();  // TODO: 실제 로그인 유저 조회로 교체

        Page<RecommendationSession> sessionPage = sessionRepository.findByUser(user, pageable);

        return sessionPage.map(session -> {
            List<RecommendationItem> items = itemRepository.findBySession(session);
            List<RecommendationItemResponse> itemResponses = items.stream()
                    .map(this::toItemResponse)
                    .toList();
            return toSessionResponse(session, itemResponses);
        });
    }

    /**
     * 추천 세션 상세 조회
     */
    @Transactional(readOnly = true)
    public RecommendationSessionResponse getSessionDetail(Long sessionId) {
        RecommendationSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("RecommendationSession not found. id=" + sessionId));

        List<RecommendationItem> items = itemRepository.findBySession(session);
        List<RecommendationItemResponse> itemResponses = items.stream()
                .map(this::toItemResponse)
                .toList();

        return toSessionResponse(session, itemResponses);
    }

    // ================== 매핑 메서드들 ==================

    private RecommendationSessionResponse toSessionResponse(
            RecommendationSession session,
            List<RecommendationItemResponse> items
    ) {
        return RecommendationSessionResponse.builder()
                .id(session.getId())
                .userId(session.getUser().getId())
                .season(session.getSeason())
                .dressMood(session.getDressMood())
                .dressSilhouette(session.getDressSilhouette())
                .weddingColor(session.getWeddingColor())
                .bouquetAtmosphere(session.getBouquetAtmosphere())
                .items(items)
                .build();
    }

    /**
     * RecommendationItem -> RecommendationItemResponse
     * - bouquetName/Price/ImageUrl 대신 BouquetResponse + categories 로 내려줌
     */
    private RecommendationItemResponse toItemResponse(RecommendationItem item) {
        Bouquet bouquet = item.getBouquet();

        // 부케 카테고리 조회
        List<BouquetCategory> categories = bouquetCategoryRepository.findByBouquet(bouquet);
        List<BouquetCategoryResponse> categoryResponses = categories.stream()
                .map(this::toCategoryResponse)
                .toList();

        // BouquetResponse 생성 (카테고리 포함)
        BouquetResponse bouquetResponse = BouquetResponse.builder()
                .id(bouquet.getId())
                .name(bouquet.getName())
                .price(bouquet.getPrice())
                .reason(bouquet.getReason())
                .description(bouquet.getDescription())
                .imageUrl(bouquet.getImageUrl())
                .categories(categoryResponses)
                .build();

        return RecommendationItemResponse.builder()
                .id(item.getId())
                .bouquetId(bouquet.getId())
                .bouquet(bouquetResponse)
                .build();
    }

    private BouquetCategoryResponse toCategoryResponse(BouquetCategory category) {
        return BouquetCategoryResponse.builder()
                .id(category.getId())
                .bouquetId(category.getBouquet().getId())
                .season(category.getSeason())
                .dressMood(category.getDressMood())
                .dressSilhouette(category.getDressSilhouette())
                .weddingColor(category.getWeddingColor())
                .bouquetAtmosphere(category.getBouquetAtmosphere())
                .usage(category.getUsage())
                .build();
    }

    // ================== 로그인 유저 조회 ==================
    private User getCurrentUser() {
        // 현재 쓰레드의 요청 정보 가져오기
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            throw new IllegalStateException("요청 컨텍스트가 없습니다. (HTTP 요청이 아닌 곳에서 호출됨)");
        }

        HttpSession session = attributes.getRequest().getSession(false);
        if (session == null) {
            throw new IllegalStateException("로그인 세션이 없습니다. (session == null)");
        }

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new IllegalStateException("세션에 userId가 없습니다. 로그인되지 않은 사용자입니다.");
        }

        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalStateException("현재 로그인한 사용자를 찾을 수 없습니다. id=" + userId));
    }

    // ================== 임시 추천 알고리즘 ==================
    /**
     * 실제 추천 알고리즘이 들어갈 자리.
     * 지금은 임시로 "전체 부케 중 상위 3개"를 가져온다.
     */
    private List<Bouquet> pickRecommendedBouquets(RecommendationSession session) {
        // TODO: session(season, dressMood, dressSilhouette, weddingColor, bouquetAtmosphere)
        //       조건을 활용한 추천 알고리즘으로 교체
        return bouquetRepository.findAll(PageRequest.of(0, 3)).getContent();
    }
}
