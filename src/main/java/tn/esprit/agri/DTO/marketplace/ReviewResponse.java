package tn.esprit.agri.DTO.marketplace;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data @Builder
public class ReviewResponse {
    private Long id;
    private Long orderId;
    private String authorName;
    private Long listingId;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
