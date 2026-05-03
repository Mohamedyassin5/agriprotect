package tn.esprit.agri.DTO.wallet;

import lombok.Builder;
import lombok.Data;
import tn.esprit.agri.entities.enums.WalletStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
public class WalletResponse {
    private Long id;
    private String userId;
    private BigDecimal availableBalance;
    private BigDecimal emergencyFundBalance;
    private BigDecimal totalBalance;
    private WalletStatus status;
    private BigDecimal emergencyTargetAmount;
    private BigDecimal emergencyMonthlyContribution;
    private boolean emergencyAutoContribute;
    private Integer emergencyContributionDay;
    private Double emergencyProgressPct;
    private String emergencyResilienceLevel;
    private LocalDateTime createdAt;
}
