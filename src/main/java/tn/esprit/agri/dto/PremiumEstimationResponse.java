package tn.esprit.agri.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder(toBuilder = true)  // ← Ajoute cette option ici !
@NoArgsConstructor
@AllArgsConstructor
public class PremiumEstimationResponse {

    private BigDecimal totalPremium;
    private Map<String, FormulaDetail> detailsByFormula;
    private String suggestedFormula;
    private BigDecimal suggestedInsuredAmount;
    private BigDecimal minAllowedInsuredAmount;
    private BigDecimal maxAllowedInsuredAmount;
    private Double aiRiskScore;           // 0.0 à 1.0 (plus bas = moins risqué)
    private String riskLevel;             // LOW | MEDIUM | HIGH | VERY_HIGH
    private List<String> riskFactors;     // Explications ("Risque élevé dû à la sécheresse récente dans la région")
    private Map<String, Object> aiInsights;

    @Data
    @Builder(toBuilder = true)   // ← Important : ajoute toBuilder = true ici aussi
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FormulaDetail {
        private String formulaName;
        private BigDecimal coveragePercentage;
        private BigDecimal franchisePercentage;
        private BigDecimal insuredAmount;
        private BigDecimal premiumAmount;
        private String shortDescription;
    }
}