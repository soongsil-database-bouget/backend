package com.dbapplication.bouget.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bouquets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Bouquet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column
    private int price;

    @Lob
    @Column
    private String reason;

    @Lob
    @Column
    private String description;

    @Column(name = "image_url", length = 255)
    private String imageUrl;
}
