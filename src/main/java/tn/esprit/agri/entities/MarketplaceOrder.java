package tn.esprit.agri.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import tn.esprit.agri.entities.enums.OrderStatus;
import tn.esprit.agri.entities.enums.PaymentSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "marketplace_order")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class MarketplaceOrder {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_source")
    private PaymentSource paymentSource;

    @Column(name = "delivery_address", length = 300)
    private String deliveryAddress;

    @Column(length = 500)
    private String notes;

    @Column(name = "wallet_transaction_id")
    private Long walletTransactionId;

    @Column(name = "tracking_number", length = 30)
    private String trackingNumber;

    @Column(name = "estimated_delivery")
    private LocalDate estimatedDelivery;

    @Column(name = "seller_note", length = 500)
    private String sellerNote;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
