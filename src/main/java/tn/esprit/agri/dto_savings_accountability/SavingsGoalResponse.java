package tn.esprit.agri.dto_savings_accountability;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsGoalResponse {
    private String id;
    private Long savingsAccountId;
    private String goalName;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private BigDecimal remainingAmount;
    private LocalDate targetDate;
    private String description;
    private Boolean achieved;
    private Boolean collected;
    private LocalDateTime collectedAt;
    private Double progressPercentage;

    /** 0 = auto, 1 = priorité haute, 2 = deuxième... */
    private Integer priority;

    /** % du solde alloué à cet objectif. null = auto. */
    private BigDecimal customAllocationPercentage;

    /** "AUTO_PROPORTIONAL", "AUTO_PRIORITY", "CUSTOM_PERCENTAGE" */
    private String allocationMode;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
