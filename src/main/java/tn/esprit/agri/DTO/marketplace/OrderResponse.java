package tn.esprit.agri.DTO.marketplace;

import lombok.Builder;
import lombok.Data;
import tn.esprit.agri.entities.enums.OrderStatus;
import tn.esprit.agri.entities.enums.PaymentSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder
public class OrderResponse {
    private Long id;
    private String buyerId;
    private String buyerName;
    private String buyerEmail;
    private Long listingId;
    private String listingTitle;
    private String listingImageUrl;
    private String sellerName;
    private String sellerEmail;
    private String sellerId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private OrderStatus status;
    private PaymentSource paymentSource;
    private String deliveryAddress;
    private String notes;
    private String sellerNote;
    private String trackingNumber;
    private LocalDate estimatedDelivery;
    private LocalDateTime createdAt;
    private boolean hasReview;
    private List<OrderTrackingEventResponse> trackingEvents;
}
