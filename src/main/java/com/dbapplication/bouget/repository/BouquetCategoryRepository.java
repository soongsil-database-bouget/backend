package com.dbapplication.bouget.repository;

import com.dbapplication.bouget.entity.Bouquet;
import com.dbapplication.bouget.entity.BouquetCategory;
import com.dbapplication.bouget.entity.enums.BouquetAtmosphere;
import com.dbapplication.bouget.entity.enums.Season;
import com.dbapplication.bouget.entity.enums.Usage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BouquetCategoryRepository extends JpaRepository<BouquetCategory, Long> {

    /**
     * 필터(시즌, 분위기, 용도)에 맞는 부케 목록 조회.
     * - 파라미터가 null이면 해당 조건은 무시
     * - distinct로 부케 중복 제거
     */
    @Query("""
        select distinct bc.bouquet
        from BouquetCategory bc
        where (:season is null or bc.season = :season)
          and (:bouquetAtmosphere is null or bc.bouquetAtmosphere = :bouquetAtmosphere)
          and (:usage is null or bc.usage = :usage)
        """)
    Page<Bouquet> findDistinctBouquetsByFilters(
            @Param("season") Season season,
            @Param("bouquetAtmosphere") BouquetAtmosphere bouquetAtmosphere,
            @Param("usage") Usage usage,
            Pageable pageable
    );

    /**
     * 특정 부케에 연결된 카테고리 전부 조회 (상세 화면에서 사용)
     */
    List<BouquetCategory> findByBouquet(Bouquet bouquet);
}
