package tn.esprit.agri.dto_savings_accountability;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsGoalRequest {
    private String goalName;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private LocalDate targetDate;
    private String description;
    private Boolean achieved;

    /**
     * Priorité de financement : 1 = premier financé, 2 = deuxième, 0 = auto (proportionnel).
     */
    private Integer priority;

    /**
     * % du solde à allouer à cet objectif (0-100). null = calcul automatique.
     */
    private BigDecimal customAllocationPercentage;
}
