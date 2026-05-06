package tn.esprit.agri.services;

import tn.esprit.agri.DTO.PremiumEstimationResponse;
import tn.esprit.agri.entities.enums.CoverageType;

import java.util.List;
import java.util.Map;

public interface IAIRiskAssessmentService {


    AICompleteEstimationResult calculateCompleteEstimation(String userId, CoverageType coverType);

    // Gardez l'ancienne méthode si nécessaire
    AIRiskAssessmentResult assessRisk(String userId, CoverageType coverType,
                                      double environmentalRisk, int userScore);

    record AIRiskAssessmentResult(
            double riskScore,
            String riskLevel,
            double recommendedAdjustment,
            List<String> keyRiskFactors,
            Map<String, Object> detailedInsights
    ) {}

    record AICompleteEstimationResult(
            Map<String, PremiumEstimationResponse.FormulaDetail> detailsByFormula,
            String suggestedFormula,
            double riskScore,
            String riskLevel,
            List<String> riskFactors,
            Map<String, Object> insights
    ) {}
}