package tn.esprit.agri.DTO.AnalyseDTO;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreditScoringDto {
    private Double revenueStabilityScore;
    private Double debtRatioScore;
    private Double projectRiskScore;
    private Double historyScore;
    private Double finalScore;
    private String recommendation;
}
