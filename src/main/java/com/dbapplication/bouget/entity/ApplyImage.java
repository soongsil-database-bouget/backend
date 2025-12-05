package com.dbapplication.bouget.entity;

import com.dbapplication.bouget.entity.enums.ApplyStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "apply_image",
        indexes = {
                @Index(
                        name = "apply_image_index_1",
                        columnList = "user_id, created_at"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ApplyImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK: user_id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // FK: bouquet_id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bouquet_id", nullable = false)
    private Bouquet bouquet;

    // FK: session_id (nullable)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private RecommendationSession session;

    @Column(name = "src_image_url", nullable = false, length = 255)
    private String srcImageUrl;

    @Column(name = "gen_image_url", nullable = false, length = 255)
    private String genImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ApplyStatus status = ApplyStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ===== 상태 변경 메서드 =====
    public void markPending() {
        this.status = ApplyStatus.PENDING;
        this.genImageUrl = "";
    }

    public void markDone(String genImageUrl) {
        this.genImageUrl = genImageUrl;
        this.status = ApplyStatus.DONE;
    }

    public void markFailed() {
        this.status = ApplyStatus.FAILED;
    }
}
