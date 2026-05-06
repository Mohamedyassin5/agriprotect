package tn.esprit.agri.DTO.marketplace;

import lombok.Data;

@Data
public class ReviewRequest {
    private Long orderId;
    private Integer rating;
    private String comment;
}
