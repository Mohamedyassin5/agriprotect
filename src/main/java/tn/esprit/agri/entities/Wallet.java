package tn.esprit.agri.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import tn.esprit.agri.entities.enums.WalletStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "wallet", indexes = @Index(name = "idx_wallet_user", columnList = "user_id"))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Wallet {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "available_balance", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(name = "emergency_fund_balance", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal emergencyFundBalance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private WalletStatus status = WalletStatus.ACTIVE;

    // Emergency fund config
    @Column(name = "emergency_target_amount", precision = 15, scale = 2)
    private BigDecimal emergencyTargetAmount;

    @Column(name = "emergency_monthly_contribution", precision = 15, scale = 2)
    private BigDecimal emergencyMonthlyContribution;

    @Column(name = "emergency_auto_contribute")
    @Builder.Default
    private boolean emergencyAutoContribute = false;

    @Column(name = "emergency_contribution_day")
    @Builder.Default
    private Integer emergencyContributionDay = 1;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WalletTransaction> transactions = new ArrayList<>();

    public BigDecimal getTotalBalance() {
        return availableBalance.add(emergencyFundBalance);
    }

    public Double getEmergencyProgressPct() {
        if (emergencyTargetAmount == null || emergencyTargetAmount.compareTo(BigDecimal.ZERO) == 0)
            return null;
        return emergencyFundBalance.divide(emergencyTargetAmount, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).doubleValue();
    }
}
