package tn.esprit.agri.DTO.marketplace;

import lombok.Builder;
import lombok.Data;
import tn.esprit.agri.entities.enums.OrderStatus;

import java.time.LocalDateTime;

@Data @Builder
public class OrderTrackingEventResponse {
    private Long id;
    private OrderStatus status;
    private String description;
    private String location;
    private LocalDateTime createdAt;
}
