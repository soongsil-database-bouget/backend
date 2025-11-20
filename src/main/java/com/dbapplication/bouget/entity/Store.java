package com.dbapplication.bouget.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "store")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK: bouquet_id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bouquet_id", nullable = false)
    private Bouquet bouquet;

    @Column(name = "store_name", nullable = false, length = 255)
    private String storeName;

    @Column(name = "store_url", length = 255)
    private String storeUrl;

    @Column(name = "instagram_id", length = 255)
    private String instagramId;
}
