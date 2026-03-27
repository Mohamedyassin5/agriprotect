package tn.esprit.agri.dto;

import lombok.*;

import java.math.BigDecimal;
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