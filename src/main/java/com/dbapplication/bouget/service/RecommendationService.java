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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final RecommendationSessionRepository sessionRepository;
    private final RecommendationItemRepository itemRepository;
    private final BouquetRepository bouquetRepository;
    private final UserRepository userRepository;
    private final BouquetCategoryRepository bouquetCategoryRepository;

    // 가중치 상수
    private static final int WEIGHT_ATMOSPHERE      = 10;
    private static final int WEIGHT_SEASON          = 9;
    private static final int WEIGHT_SILHOUETTE      = 8;
    private static final int WEIGHT_DRESS_MOOD      = 7;
    private static final int WEIGHT_WEDDING_COLOR   = 7;


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
        User user = getCurrentUser();

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
        BouquetCategory categories = bouquetCategoryRepository.findByBouquet(bouquet);
        BouquetCategoryResponse categoryResponses = toCategoryResponse(categories);

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

    // ================== 추천 알고리즘 ==================
    /**
     * "전체 부케 중 유사도 높은 3개"를 가져온다.
     */
    private List<Bouquet> pickRecommendedBouquets(RecommendationSession session) {

        // 세션 기준으로 나올 수 있는 최대 점수 (분모용)
        int maxScore = calculateMaxScore(session);

        List<Bouquet> allBouquets = bouquetRepository.findAll();

        List<ScoredBouquet> scored = allBouquets.stream()
                .map(bouquet -> new ScoredBouquet(
                        bouquet,
                        calculateScore(session, bouquet)
                ))
                .sorted(Comparator
                        .comparingInt(ScoredBouquet::score).reversed()
                        .thenComparing(sb -> sb.bouquet().getId()))
                .toList();

        // 0점 초과인 애들 위주로 최대 3개
        List<ScoredBouquet> initiallyPicked = scored.stream()
                .filter(sb -> sb.score() > 0)
                .limit(3)
                .toList();

        List<Bouquet> result = initiallyPicked.stream()
                .map(ScoredBouquet::bouquet)
                .collect(Collectors.toCollection(ArrayList::new));

        int beforeSize = result.size();

        // 추천이 3개보다 적으면, 점수와 상관 없이 채워 넣기
        if (result.size() < 3) {
            int need = 3 - result.size();

            Set<Long> alreadyPickedIds = result.stream()
                    .map(Bouquet::getId)
                    .collect(Collectors.toSet());

            scored.stream()
                    .map(ScoredBouquet::bouquet)
                    .filter(b -> !alreadyPickedIds.contains(b.getId()))
                    .limit(need)
                    .forEach(result::add);

            int added = result.size() - beforeSize;

            if (added > 0) {
                log.info(
                        "[BouquetRecommendation] 추천 부케가 3개 미만이라 {}개를 점수와 무관하게 임의로 추가했습니다. (before={}, after={})",
                        added, beforeSize, result.size()
                );
            }
        }

        // === 최종 추천 결과 로그 (정확도 측정용) ===
        log.info("[BouquetRecommendation] sessionId={} 추천 결과 (추천 개수: {}, 최대점수: {})",
                session.getId(), result.size(), maxScore);

        for (Bouquet bouquet : result) {
            int score = calculateScore(session, bouquet);

            // maxScore 기준으로 분모/분자 로그
            if (maxScore > 0) {
                log.info(
                        "[BouquetRecommendation] 추천 bouquetId={} score={}/{} (정확도 비율: {}%)",
                        bouquet.getId(),
                        score,
                        maxScore,
                        Math.round(score * 100.0 / maxScore)
                );
            } else {
                log.info(
                        "[BouquetRecommendation] 추천 bouquetId={} score=0/0 (세션 조건이 없어 최대점수가 0)",
                        bouquet.getId()
                );
            }
        }

        return result;
    }

    /**
     * 세션 조건과 부케 정보를 비교해서 점수 계산
     */
    private int calculateScore(RecommendationSession session, Bouquet bouquet) {
        int score = 0;
        BouquetCategory categories = bouquetCategoryRepository.findByBouquet(bouquet);

        if (session.getBouquetAtmosphere() != null
                && session.getBouquetAtmosphere() == categories.getBouquetAtmosphere()) {
            score += WEIGHT_ATMOSPHERE;
        }

        if (session.getSeason() != null
                && categories.getSeason() != null
                && session.getSeason() == categories.getSeason()) {
            score += WEIGHT_SEASON;
        }

        if (session.getDressSilhouette() != null
                && categories.getDressSilhouette() != null
                && session.getDressSilhouette() == categories.getDressSilhouette()) {
            score += WEIGHT_SILHOUETTE;
        }

        if (session.getDressMood() != null
                && categories.getDressMood() != null
                && session.getDressMood() == categories.getDressMood()) {
            score += WEIGHT_DRESS_MOOD;
        }

        if (session.getWeddingColor() != null
                && categories.getWeddingColor() != null
                && session.getWeddingColor() == categories.getWeddingColor()) {
            score += WEIGHT_WEDDING_COLOR;
        }

        return score;
    }

    /**
     * 분모용: 세션이 가진 조건 기준으로 "이론상 최대 점수"
     * (세션에서 null인 조건은 분모에서 제외해서 정규화)
     */
    private int calculateMaxScore(RecommendationSession session) {
        int max = 0;

        if (session.getBouquetAtmosphere() != null) max += WEIGHT_ATMOSPHERE;
        if (session.getSeason() != null)           max += WEIGHT_SEASON;
        if (session.getDressSilhouette() != null)  max += WEIGHT_SILHOUETTE;
        if (session.getDressMood() != null)        max += WEIGHT_DRESS_MOOD;
        if (session.getWeddingColor() != null)     max += WEIGHT_WEDDING_COLOR;

        return max;
    }

    /**
     * 부케 + 점수 묶음용 내부 레코드
     */
    private record ScoredBouquet(Bouquet bouquet, int score) {}
}
