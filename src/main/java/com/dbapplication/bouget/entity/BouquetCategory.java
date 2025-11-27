package com.dbapplication.bouget.entity;

import com.dbapplication.bouget.entity.enums.*;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bouquet_categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BouquetCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK: bouquet_id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bouquet_id", nullable = false)
    private Bouquet bouquet;

    @Enumerated(EnumType.STRING)
    @Column
    private Season season;

    @Enumerated(EnumType.STRING)
    @Column(name = "dress_mood")
    private DressMood dressMood;

    @Enumerated(EnumType.STRING)
    @Column(name = "dress_silhouette")
    private DressSilhouette dressSilhouette;

    @Enumerated(EnumType.STRING)
    @Column(name = "wedding_color")
    private WeddingColor weddingColor;

    @Enumerated(EnumType.STRING)
    @Column(name = "bouquet_atmosphere")
    private BouquetAtmosphere bouquetAtmosphere;

    @Enumerated(EnumType.STRING)
    @Column
    private Usage usage;
}
