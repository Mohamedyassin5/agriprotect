package tn.esprit.agri.DTO.marketplace;

import lombok.Builder;
import lombok.Data;
import tn.esprit.agri.entities.enums.ListingCategory;
import tn.esprit.agri.entities.enums.ListingStatus;
import tn.esprit.agri.entities.enums.ListingType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder
public class ListingResponse {
    private Long id;
    private String sellerId;
    private String sellerName;
    private String title;
    private String description;
    private ListingCategory category;
    private ListingType type;
    private BigDecimal price;
    private String unit;
    private Integer stock;
    private List<String> images;
    private ListingStatus status;
    private String location;
    private String adminNote;
    private Integer viewCount;
    private Double averageRating;
    private Integer reviewCount;
    private LocalDateTime createdAt;
}
