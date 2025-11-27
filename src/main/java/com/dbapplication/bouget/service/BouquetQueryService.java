package com.dbapplication.bouget.service;

import com.dbapplication.bouget.dto.BouquetCategoryResponse;
import com.dbapplication.bouget.dto.BouquetDetailResponse;
import com.dbapplication.bouget.dto.BouquetResponse;
import com.dbapplication.bouget.dto.StoreResponse;
import com.dbapplication.bouget.entity.Bouquet;
import com.dbapplication.bouget.entity.BouquetCategory;
import com.dbapplication.bouget.entity.Store;
import com.dbapplication.bouget.entity.enums.BouquetAtmosphere;
import com.dbapplication.bouget.entity.enums.Season;
import com.dbapplication.bouget.entity.enums.Usage;
import com.dbapplication.bouget.repository.BouquetCategoryRepository;
import com.dbapplication.bouget.repository.BouquetRepository;
import com.dbapplication.bouget.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BouquetQueryService {

    private final BouquetRepository bouquetRepository;
    private final BouquetCategoryRepository bouquetCategoryRepository;
    private final StoreRepository storeRepository;

    /**
     * 부케 리스트 조회 (전체 / 필터)
     * - season, bouquetAtmosphere, usage 중 하나라도 들어오면 카테고리 테이블 기반으로 필터
     * - 전부 null이면 bouquets 테이블 전체 조회
     */
    @Transactional(readOnly = true)
    public Page<BouquetResponse> getBouquets(
            Season season,
            BouquetAtmosphere bouquetAtmosphere,
            Usage usage,
            Pageable pageable
    ) {
        Page<Bouquet> bouquetPage;

        boolean hasFilter = (season != null) || (bouquetAtmosphere != null) || (usage != null);

        if (hasFilter) {
            // bouquet_categories 기준으로 join 조회
            bouquetPage = bouquetCategoryRepository.findDistinctBouquetsByFilters(
                    season,
                    bouquetAtmosphere,
                    usage,
                    pageable
            );
        } else {
            // 전체 조회
            bouquetPage = bouquetRepository.findAll(pageable);
        }

        // 엔티티 → DTO 매핑 (이제 각 BouquetResponse에 categories까지 포함)
        return bouquetPage.map(this::toBouquetResponse);
    }

    /**
     * 부케 상세 조회
     */
    @Transactional(readOnly = true)
    public BouquetDetailResponse getBouquetDetail(Long bouquetId) {
        Bouquet bouquet = bouquetRepository.findById(bouquetId)
                .orElseThrow(() -> new IllegalArgumentException("Bouquet not found. id=" + bouquetId));

        // 이 부케에 연결된 카테고리
        BouquetCategory categories = bouquetCategoryRepository.findByBouquet(bouquet);
        BouquetCategoryResponse categoryResponses = toCategoryResponse(categories);

        Store store = storeRepository.findByBouquet(bouquet);

        StoreResponse storeResponse = null;
        if (store != null) {
            storeResponse = toStoreResponse(store);
        }

        return BouquetDetailResponse.builder()
                .id(bouquet.getId())
                .name(bouquet.getName())
                .price(bouquet.getPrice())
                .reason(bouquet.getReason())
                .description(bouquet.getDescription())
                .imageUrl(bouquet.getImageUrl())
                .categories(categoryResponses)
                .store(storeResponse)
                .build();
    }

    // ====== 매핑 메서드들 ======

    /**
     * 리스트/추천/virtual-fittings 등에서 사용할 BouquetResponse 생성
     * - 부케 기본 정보 + categories 포함
     */
    private BouquetResponse toBouquetResponse(Bouquet bouquet) {
        BouquetCategory categories = bouquetCategoryRepository.findByBouquet(bouquet);
        BouquetCategoryResponse categoryResponses = toCategoryResponse(categories);


        return BouquetResponse.builder()
                .id(bouquet.getId())
                .name(bouquet.getName())
                .price(bouquet.getPrice())
                .reason(bouquet.getReason())
                .description(bouquet.getDescription())
                .imageUrl(bouquet.getImageUrl())
                .categories(categoryResponses)
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
    private StoreResponse toStoreResponse(Store store) {
        return StoreResponse.builder()
                .id(store.getId())
                .bouquetId(store.getBouquet().getId())
                .storeName(store.getStoreName())
                .storeUrl(store.getStoreUrl())
                .instagramId(store.getInstagramId())
                .build();
    }

}
