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
public class EmergencyFundCalculatorResponse {

    // --- Données calculées depuis vos écritures comptables réelles ---
    private BigDecimal totalExpensesLast6Months;   // Somme brute des dépenses sur 6 mois
    private int monthsWithExpenseData;             // Nombre de mois ayant eu des dépenses
    private LocalDate periodFrom;                  // Début de la période analysée
    private LocalDate periodTo;                    // Fin de la période analysée
    private List<MonthlyExpenseDetail> monthlyExpenseDetails; // Détail mois par mois

    private BigDecimal averageMonthlyExpenses;      // Moyenne mensuelle réelle

    // --- Seuils recommandés ---
    private BigDecimal recommendedMinimum;          // Minimum : 3 mois de dépenses
    private BigDecimal recommendedOptimal;          // Optimal : 6 mois de dépenses

    // --- Situation actuelle ---
    private BigDecimal currentSavings;              // Solde actuel du compte épargne
    private BigDecimal deficit;                     // Manque pour atteindre le minimum (0 si déjà OK)
    private BigDecimal surplusAboveOptimal;         // Excédent au-dessus du seuil optimal (0 sinon)

    // --- Indicateurs ---
    private double coverageMonths;                  // Mois de dépenses couverts par l'épargne actuelle
    private String protectionLevel;                 // NONE | LOW | MODERATE | GOOD | EXCELLENT
    private double protectionPercentage;            // % de l'objectif optimal atteint

    // --- Plan pour atteindre les seuils ---
    private BigDecimal monthlyContributionNeeded3Months; // Pour atteindre 3 mois de fonds en 6 mois
    private BigDecimal monthlyContributionNeeded6Months; // Pour atteindre 6 mois de fonds en 12 mois

    private String recommendation;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MonthlyExpenseDetail {
        private String month;               // Ex: "MARCH 2025"
        private BigDecimal totalExpenses;   // Total des dépenses ce mois-là (données réelles)
        private int numberOfEntries;        // Nombre d'écritures de dépenses ce mois-là
    }
}
