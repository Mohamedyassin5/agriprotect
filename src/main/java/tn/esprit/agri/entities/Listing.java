package tn.esprit.agri.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import tn.esprit.agri.entities.enums.ListingCategory;
import tn.esprit.agri.entities.enums.ListingStatus;
import tn.esprit.agri.entities.enums.ListingType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "marketplace_listing")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Listing {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ListingCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ListingType type;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(length = 30)
    private String unit;

    @Column(nullable = false)
    @Builder.Default
    private Integer stock = 1;

    @ElementCollection
    @CollectionTable(name = "listing_images", joinColumns = @JoinColumn(name = "listing_id"))
    @Column(name = "image_url", length = 500)
    @Builder.Default
    private List<String> images = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ListingStatus status = ListingStatus.PENDING;

    @Column(length = 100)
    private String location;

    @Column(name = "admin_note", length = 500)
    private String adminNote;

    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "average_rating")
    private Double averageRating;

    @Column(name = "review_count")
    @Builder.Default
    private Integer reviewCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
