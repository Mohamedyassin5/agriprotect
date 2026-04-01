package tn.esprit.agri.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.agri.dto.PremiumEstimationResponse;
import tn.esprit.agri.entities.Crop;
import tn.esprit.agri.entities.CropReference;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.entities.enums.CoverageType;
import tn.esprit.agri.repositories.CropReferenceRepository;
import tn.esprit.agri.repositories.UserRepository;
import tn.esprit.agri.services.IAIRiskAssessmentService;
import tn.esprit.agri.services.IPremiumEstimationService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PremiumEstimationServiceImpl implements IPremiumEstimationService {

    private final UserRepository userRepository;
    private final CropReferenceRepository cropReferenceRepository;
    private final IAIRiskAssessmentService aiRiskService;

    private static final Map<String, PremiumEstimationResponse.FormulaDetail> FORMULAS = Map.of(
            "BASIC",    PremiumEstimationResponse.FormulaDetail.builder()
                    .formulaName("BASIC").coveragePercentage(BigDecimal.valueOf(0.60))
                    .franchisePercentage(BigDecimal.valueOf(0.30)).shortDescription("Min.").build(),
            "STANDARD", PremiumEstimationResponse.FormulaDetail.builder()
                    .formulaName("STANDARD").coveragePercentage(BigDecimal.valueOf(0.75))
                    .franchisePercentage(BigDecimal.valueOf(0.20)).shortDescription("Équilibre").build(),
            "PREMIUM",  PremiumEstimationResponse.FormulaDetail.builder()
                    .formulaName("PREMIUM").coveragePercentage(BigDecimal.valueOf(0.90))
                    .franchisePercentage(BigDecimal.valueOf(0.10)).shortDescription("Max.").build()
    );

    // ==================== Facteurs de risque statiques ====================
    private double calculateTemperatureRiskFactor(Crop crop) {
        double avg = crop.getAverageTemperature() != null ? crop.getAverageTemperature() : 0;
        double min = crop.getMinTemperature() != null ? crop.getMinTemperature() : 0;
        double max = crop.getMaxTemperature() != null ? crop.getMaxTemperature() : 0;

        if (min == 0 && max == 0) return 1.0;
        if (avg < min || avg > max) {
            double deviation = Math.max(Math.abs(avg - min), Math.abs(avg - max));
            return Math.min(1.0 + (deviation / 5.0) * 0.15, 1.60);
        }
        return 1.0;
    }

    private double calculateHumidityRiskFactor(Crop crop) {
        double optimal = crop.getOptimalHumidity() != null ? crop.getOptimalHumidity() : 0;
        double minH = crop.getMinHumidity() != null ? crop.getMinHumidity() : 0;
        double maxH = crop.getMaxHumidity() != null ? crop.getMaxHumidity() : 0;

        if (optimal == 0) return 1.0;

        double current = (minH + maxH) / 2.0;
        double distance = Math.abs(current - optimal);

        if (distance > 15) {
            return 1.0 + Math.min((distance - 15) / 20.0 * 0.20, 0.20);
        }
        return 1.0;
    }

    private double calculateSoilRiskFactor(Crop crop) {
        String soil = crop.getTypeterres() != null ? crop.getTypeterres().toLowerCase() : "";
        String cropType = crop.getCropType() != null ? crop.getCropType().toLowerCase() : "";

        if (cropType.contains("olive")) {
            if (soil.contains("calcaire") || soil.contains("argileuse") || soil.contains("limoneuse")) return 1.0;
            if (soil.contains("sableuse")) return 1.15;
            return 1.25;
        }

        if (cropType.contains("cereal") || cropType.contains("blé") || cropType.contains("orge")) {
            if (soil.contains("argileuse") || soil.contains("limoneuse")) return 1.0;
            if (soil.contains("sableuse")) return 1.20;
            return 1.30;
        }
        return 1.10;
    }

    @Override
    public PremiumEstimationResponse calculateEstimation(String userId, CoverageType selectedCoverType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (user.getCrops().isEmpty()) {
            throw new RuntimeException("Aucune culture déclarée");
        }

        String selectedFormula = (selectedCoverType != null) ? selectedCoverType.name() : "STANDARD";
        int currentYear = LocalDate.now().getYear();

        Map<String, PremiumEstimationResponse.FormulaDetail> details = new LinkedHashMap<>();
        BigDecimal totalPremium = BigDecimal.ZERO;

        // Calcul classique
        for (String formula : FORMULAS.keySet()) {
            var config = FORMULAS.get(formula);
            BigDecimal formulaInsured = BigDecimal.ZERO;
            BigDecimal formulaPremium = BigDecimal.ZERO;

            for (Crop crop : user.getCrops()) {
                CropReference ref = cropReferenceRepository
                        .findByCropTypeAndReferenceYear(crop.getCropType(), currentYear)
                        .orElseGet(() -> cropReferenceRepository
                                .findTopByCropTypeOrderByReferenceYearDesc(crop.getCropType())
                                .orElseThrow(() -> new RuntimeException("No reference for " + crop.getCropType())));

                BigDecimal baseInsured = BigDecimal.valueOf(crop.getSurface())
                        .multiply(BigDecimal.valueOf(ref.getReferenceYield()))
                        .multiply(BigDecimal.valueOf(ref.getReferencePrice()))
                        .setScale(2, RoundingMode.HALF_UP);

                BigDecimal coverage = config.getCoveragePercentage();
                BigDecimal covered = baseInsured.multiply(coverage);

                BigDecimal cropPremium = covered
                        .multiply(BigDecimal.valueOf(ref.getBasePremiumRate()))
                        .setScale(2, RoundingMode.HALF_UP);

                double tempFactor = calculateTemperatureRiskFactor(crop);
                double humidityFactor = calculateHumidityRiskFactor(crop);
                double soilFactor = calculateSoilRiskFactor(crop);
                double environmentalRisk = tempFactor * humidityFactor * soilFactor;

                BigDecimal riskAdj = BigDecimal.ONE.add(
                        BigDecimal.valueOf(user.getScore() != null ? user.getScore() - 50 : 0)
                                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(0.5))
                );

                BigDecimal finalMultiplier = riskAdj.multiply(BigDecimal.valueOf(environmentalRisk));
                cropPremium = cropPremium.multiply(finalMultiplier);

                formulaInsured = formulaInsured.add(covered);
                formulaPremium = formulaPremium.add(cropPremium);
            }

            details.put(formula, config.toBuilder()
                    .insuredAmount(formulaInsured)
                    .premiumAmount(formulaPremium)
                    .build());

            if (formula.equals(selectedFormula)) {
                totalPremium = formulaPremium;
            }
        }

        // ==================== IA avec diagnostic ====================
        IAIRiskAssessmentService.AIRiskAssessmentResult aiResult = getAiRiskResult(user, selectedCoverType);

        BigDecimal finalPremium = totalPremium
                .multiply(BigDecimal.valueOf(aiResult.recommendedAdjustment()))
                .setScale(2, RoundingMode.HALF_UP);

        String suggested = (user.getScore() != null && user.getScore() <= 55) ? "PREMIUM" :
                (user.getScore() != null && user.getScore() >= 75 ? "BASIC" : "STANDARD");

        BigDecimal suggestedInsured = details.get(suggested).getInsuredAmount();

        return PremiumEstimationResponse.builder()
                .totalPremium(finalPremium)
                .detailsByFormula(details)
                .suggestedFormula(suggested)
                .suggestedInsuredAmount(suggestedInsured)
                .minAllowedInsuredAmount(suggestedInsured.multiply(BigDecimal.valueOf(0.8)))
                .maxAllowedInsuredAmount(suggestedInsured.multiply(BigDecimal.valueOf(1.2)))
                .aiRiskScore(aiResult.riskScore())
                .riskLevel(aiResult.riskLevel())
                .riskFactors(aiResult.keyRiskFactors())
                .aiInsights(aiResult.detailedInsights())
                .build();
    }

    private IAIRiskAssessmentService.AIRiskAssessmentResult getAiRiskResult(User user, CoverageType coverType) {
        if (aiRiskService == null) {
            log.warn("Service IA non injecté → fallback");
            return createFallback();
        }

        try {
            log.info("Appel de l'analyse IA pour user {}", user.getId());
            // CORRECTION : passer l'ID au lieu de l'objet User
            IAIRiskAssessmentService.AIRiskAssessmentResult result = aiRiskService.assessRisk(user.getId(), coverType);
            log.info("IA réussie → RiskScore={}, Adjustment={}", result.riskScore(), result.recommendedAdjustment());
            return result;
        } catch (Exception e) {
            log.error("ÉCHEC IA - Cause probable : clé OpenAI invalide, quota dépassé ou problème réseau", e);
            return createFallback();
        }
    }

    private IAIRiskAssessmentService.AIRiskAssessmentResult createFallback() {
        return new IAIRiskAssessmentService.AIRiskAssessmentResult(
                0.55,
                "MEDIUM",
                1.0,
                List.of("Analyse IA temporairement indisponible"),
                Map.of("explanation", "Utilisation des facteurs de risque classiques")
        );
    }
}