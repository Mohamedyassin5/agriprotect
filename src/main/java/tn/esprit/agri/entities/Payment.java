package tn.esprit.agri.entities;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.agri.entities.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insurance_id", nullable = false)
    private Insurance insurance;

    private String paymentIntentId;     // ID Stripe

    private BigDecimal amount;          // Montant payé (TND)

    private LocalDateTime paymentDate;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;       // SUCCEEDED, FAILED, REFUNDED

    private String paymentMode;         // MONTHLY, QUARTERLY...

    private BigDecimal penaltyAmount;   // Pénalité appliquée (si existante)

    @Column(columnDefinition = "TEXT")
    private String metadata;            // Informations supplémentaires
}

