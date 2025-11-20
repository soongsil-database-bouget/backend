package com.dbapplication.bouget.entity;

import com.dbapplication.bouget.entity.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "recommendation_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RecommendationSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK: user_id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Season season;

    @Enumerated(EnumType.STRING)
    @Column(name = "dress_mood", nullable = false)
    private DressMood dressMood;

    @Enumerated(EnumType.STRING)
    @Column(name = "dress_silhouette", nullable = false)
    private DressSilhouette dressSilhouette;

    @Enumerated(EnumType.STRING)
    @Column(name = "wedding_color", nullable = false)
    private WeddingColor weddingColor;

    @Enumerated(EnumType.STRING)
    @Column(name = "bouquet_atmosphere", nullable = false)
    private BouquetAtmosphere bouquetAtmosphere;

}
