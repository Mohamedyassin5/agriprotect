package tn.esprit.agri.entities;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.agri.entities.enums.CoverageType;
import tn.esprit.agri.entities.enums.InsuranceStatus;
import tn.esprit.agri.entities.enums.PaymentMode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "insurances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Insurance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String policyNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CoverageType coverageType;

    @Column(nullable = false)
    private BigDecimal insuredAmount;

    @Column(nullable = false)
    private BigDecimal premiumAmount;

    // === CHAMPS POUR LE PAIEMENT ===
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMode paymentMode;

    @Column(nullable = false)
    private BigDecimal totalPremium;

    @Column(nullable = false)
    private BigDecimal amountPerPayment;

    @Column(nullable = true)
    private Integer numberOfPayments;

    @Column(nullable = true)
    private Integer remainingPayments;

    private LocalDate lastPaymentDate;

    /** ====================== CHAMP AJOUTÉ ====================== */
    @Column(name = "payment_intent_id", length = 255)
    private String paymentIntentId;          // ← CE CHAMP MANQUAIT !

    private LocalDate nextPaymentDue;

    // === DATES ET STATUT ===
    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InsuranceStatus status = InsuranceStatus.PENDING_SIGNATURE;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime signedAt;
    private String signedByName;

    @Column(length = 64)
    private String signToken;
    private LocalDateTime signTokenExpiry;

    @Lob
    @Column(columnDefinition = "MEDIUMBLOB")
    private byte[] signatureImage;

}