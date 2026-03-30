package tn.esprit.agri.dto_savings_accountability;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalAchievementPredictionResponse {

    // --- Vue globale du compte ---
    private BigDecimal currentBalance;
    private BigDecimal averageMonthlySavings;   // Moyenne pondérée des 7 derniers mois
    private int monthsAnalyzed;                 // Nombre de mois avec des transactions réelles
    private String dataExplanation;             // Explication de la méthode de calcul

    // --- Prédiction par objectif ---
    private List<GoalPrediction> goalPredictions;

    // --- Scénarios globaux (basés sur la moyenne mensuelle) ---
    private List<Scenario> scenarios;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GoalPrediction {
        private String goalId;
        private String goalName;
        private Integer priority;               // 0 = auto, 1+ = priorité
        private String allocationMode;          // AUTO_PROPORTIONAL | AUTO_PRIORITY | CUSTOM_PERCENTAGE
        private BigDecimal customAllocationPct; // % fixe si défini

        private BigDecimal targetAmount;
        private BigDecimal currentAmount;
        private BigDecimal remaining;
        private double progressPercentage;
        private LocalDate targetDate;

        // Prédiction spécifique à cet objectif
        private BigDecimal estimatedMonthlyContribution; // Part mensuelle qui va à cet objectif
        private Integer estimatedMonthsToComplete;        // null si objectif déjà atteint
        private LocalDate estimatedCompletionDate;        // null si déjà atteint
        private double probability;                       // 0-100
        private String status;                            // ON_TRACK | AT_RISK | ACHIEVED | BLOCKED
        private String statusDetail;                      // Explication du statut
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Scenario {
        private String name;
        private BigDecimal monthlySavings;
        private LocalDate achievementDate;      // Date d'atteinte du dernier objectif actif
        private int monthsRequired;
        private String description;
    }
}
