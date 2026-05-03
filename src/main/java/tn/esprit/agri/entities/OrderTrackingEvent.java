package tn.esprit.agri.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import tn.esprit.agri.entities.enums.OrderStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_tracking_events")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderTrackingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private MarketplaceOrder order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(length = 400)
    private String description;

    @Column(length = 150)
    private String location;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
