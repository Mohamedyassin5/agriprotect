    package tn.esprit.agri.services.impl;

    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.stereotype.Service;
    import tn.esprit.agri.DTO.PremiumEstimationResponse;
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
    import java.util.ArrayList;
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

        /**
         * FIX 3 : Gardes null explicites sur les 3 champs température.
         * Si l'un est null ou si min=0 ET max=0 (valeur par défaut JPA), on retourne 1.0 sans pénalité.
         */
        private double calculateTemperatureRiskFactor(Crop crop) {
            // FIX: null-check explicite avant toute comparaison
            if (crop.getMinTemperature() == null || crop.getMaxTemperature() == null
                    || crop.getAverageTemperature() == null) {
                return 1.0;
            }

            double avg = crop.getAverageTemperature();
            double min = crop.getMinTemperature();
            double max = crop.getMaxTemperature();

            // FIX: évite les valeurs par défaut JPA (0.0f stockés en base sans saisie)
            if (min == 0.0 && max == 0.0) return 1.0;

            if (avg < min || avg > max) {
                double deviation = Math.max(Math.abs(avg - min), Math.abs(avg - max));
                return Math.min(1.0 + (deviation / 5.0) * 0.15, 1.60);
            }
            return 1.0;
        }

        private double calculateHumidityRiskFactor(Crop crop) {
            double optimal = crop.getOptimalHumidity() != null ? crop.getOptimalHumidity() : 0;
            double minH    = crop.getMinHumidity()     != null ? crop.getMinHumidity()     : 0;
            double maxH    = crop.getMaxHumidity()     != null ? crop.getMaxHumidity()     : 0;

            if (optimal == 0) return 1.0;
            double current  = (minH + maxH) / 2.0;
            double distance = Math.abs(current - optimal);

            if (distance > 15) {
                return 1.0 + Math.min((distance - 15) / 20.0 * 0.20, 0.20);
            }
            return 1.0;
        }

        /**
         * FIX 2 : Ajout de "céréale" dans la condition des cultures céréalières.
         * Sans ce fix, "céréale" tombait dans le cas générique (return 1.10)
         * alors qu'un sol argilo-limoneux devrait donner 1.0.
         */
        private double calculateSoilRiskFactor(Crop crop) {
            String soil     = crop.getTypeterres() != null ? crop.getTypeterres().toLowerCase() : "";
            String cropType = crop.getCropType()   != null ? crop.getCropType().toLowerCase()   : "";

            if (cropType.contains("olive")) {
                if (soil.contains("calcaire") || soil.contains("argileuse") || soil.contains("limoneuse")) return 1.0;
                if (soil.contains("sableuse")) return 1.15;
                return 1.25;
            }

            // FIX: ajout de "céréale" (accent) pour matcher le type générique de la DB
            if (cropType.contains("céréale") || cropType.contains("cereal")
                    || cropType.contains("blé") || cropType.contains("orge")) {
                if (soil.contains("argileuse") || soil.contains("limoneuse")) return 1.0;
                if (soil.contains("sableuse")) return 1.20;
                // Sol non renseigné ou autre → pénalité modérée (pas 1.30 comme avant)
                return 1.10;
            }

            // Cultures maraîchères, oléagineux, etc.
            if (cropType.contains("tomate") || cropType.contains("fraise") || cropType.contains("poivron")
                    || cropType.contains("pomme de terre") || cropType.contains("oignon")) {
                if (soil.contains("limoneuse") || soil.contains("argileuse")) return 1.0;
                if (soil.contains("sableuse")) return 1.10;
                return 1.15;
            }

            return 1.10; // valeur générique pour les autres cultures
        }

        // ==================== MÉTHODE PARTAGÉE : calcul de base ====================

        /**
         * Calcule la prime de base pour une culture donnée selon une formule (BASIC/STANDARD/PREMIUM).
         * Formule : surface(ha) × rendement(T/ha) × prix(DT/T) × couverture × tauxPrime
         */
        public BigDecimal computeBasePremium(Crop crop, CropReference ref, double coveragePct) {
            BigDecimal baseInsured = BigDecimal.valueOf(crop.getSurface())
                    .multiply(BigDecimal.valueOf(ref.getReferenceYield()))
                    .multiply(BigDecimal.valueOf(ref.getReferencePrice()))
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal covered = baseInsured.multiply(BigDecimal.valueOf(coveragePct));

            return covered
                    .multiply(BigDecimal.valueOf(ref.getBasePremiumRate()))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        /**
         * Calcule le montant assuré (couvert) pour une culture donnée.
         */
        public BigDecimal computeInsuredAmount(Crop crop, CropReference ref, double coveragePct) {
            BigDecimal baseInsured = BigDecimal.valueOf(crop.getSurface())
                    .multiply(BigDecimal.valueOf(ref.getReferenceYield()))
                    .multiply(BigDecimal.valueOf(ref.getReferencePrice()))
                    .setScale(2, RoundingMode.HALF_UP);
            return baseInsured.multiply(BigDecimal.valueOf(coveragePct)).setScale(2, RoundingMode.HALF_UP);
        }

        // ==================== Génération de la raison de recommandation ====================

        public String buildRecommendationReason(String formula, User user, double environmentalRisk, String aiRiskLevel) {
            List<String> reasons = new ArrayList<>();
            Float score = user.getScore() != null ? user.getScore() : 50.0f;

            // Raisons liées au score agriculteur
            if ("PREMIUM".equals(formula)) {
                if (score <= 40) reasons.add("Votre score d'expérience est faible (" + score.intValue() + "/100) : une couverture maximale réduit votre exposition aux risques.");
                else if (score <= 55) reasons.add("Score d'expérience modéré (" + score.intValue() + "/100) : la couverture PREMIUM offre la meilleure protection.");
            } else if ("BASIC".equals(formula)) {
                if (score >= 75) reasons.add("Votre score d'expérience élevé (" + score.intValue() + "/100) indique une bonne maîtrise agricole.");
                if (score >= 85) reasons.add("Historique solide : la formule BASIC est suffisante pour votre profil.");
            } else { // STANDARD
                if (score > 55 && score < 75) reasons.add("Score intermédiaire (" + score.intValue() + "/100) : la formule STANDARD offre un bon équilibre coût/protection.");
            }

            // Raisons liées au risque IA
            if ("HIGH".equals(aiRiskLevel) || "VERY_HIGH".equals(aiRiskLevel)) {
                reasons.add("Niveau de risque évalué élevé par l'analyse IA : une couverture renforcée est recommandée.");
            } else if ("LOW".equals(aiRiskLevel) && "BASIC".equals(formula)) {
                reasons.add("L'analyse IA indique un risque faible pour votre exploitation.");
            } else if ("MEDIUM".equals(aiRiskLevel) && "STANDARD".equals(formula)) {
                reasons.add("L'analyse IA indique un risque modéré, adapté à la formule STANDARD.");
            }

            // Raisons liées au risque environnemental
            if (environmentalRisk > 1.20 && !"BASIC".equals(formula)) {
                reasons.add(String.format("Risque environnemental élevé (×%.2f) lié aux conditions de sol, température et humidité.", environmentalRisk));
            } else if (environmentalRisk <= 1.05) {
                reasons.add("Conditions environnementales favorables (température, humidité, type de sol).");
            }

            // Raisons liées aux cultures sensibles
            boolean hasSensitiveCrop = user.getCrops().stream().anyMatch(c -> {
                String ct = c.getCropType() != null ? c.getCropType().toLowerCase() : "";
                return ct.contains("tomate") || ct.contains("fraise") || ct.contains("poivron");
            });
            if (hasSensitiveCrop && "PREMIUM".equals(formula)) {
                reasons.add("Présence de cultures sensibles (tomate, fraise, poivron) nécessitant une protection accrue.");
            }

            boolean hasLargeSurface = user.getCrops().stream().anyMatch(c -> c.getSurface() != null && c.getSurface() > 10);
            if (hasLargeSurface && !"BASIC".equals(formula)) {
                reasons.add("Grande surface cultivée : le capital assuré justifie une couverture solide.");
            }

            // Fallback si aucune raison construite
            if (reasons.isEmpty()) {
                reasons.add(switch (formula) {
                    case "PREMIUM"  -> "Protection maximale recommandée au regard de votre profil de risque global.";
                    case "STANDARD" -> "Formule équilibrée correspondant à votre profil agriculteur.";
                    default         -> "Formule économique adaptée à votre faible exposition au risque.";
                });
            }

            return String.join(" ", reasons);
        }

        // ==================== Calcul principal ====================

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
            double avgEnvRisk = 0.0;
            int cropCount = 0;

            // ==================== Calcul par formule ====================
            for (String formula : List.of("BASIC", "STANDARD", "PREMIUM")) {
                var config = FORMULAS.get(formula);
                BigDecimal formulaInsured = BigDecimal.ZERO;
                BigDecimal formulaPremium = BigDecimal.ZERO;

                for (Crop crop : user.getCrops()) {
                    CropReference ref = cropReferenceRepository
                            .findByCropTypeAndReferenceYear(crop.getCropType(), currentYear)
                            .orElseGet(() -> cropReferenceRepository
                                    .findTopByCropTypeOrderByReferenceYearDesc(crop.getCropType())
                                    .orElseThrow(() -> new RuntimeException("Aucune référence pour " + crop.getCropType())));

                    double coveragePct = config.getCoveragePercentage().doubleValue();

                    BigDecimal insuredAmount = computeInsuredAmount(crop, ref, coveragePct);
                    BigDecimal cropPremium   = computeBasePremium(crop, ref, coveragePct);

                    double tempFactor        = calculateTemperatureRiskFactor(crop);
                    double humidityFactor    = calculateHumidityRiskFactor(crop);
                    double soilFactor        = calculateSoilRiskFactor(crop);
                    double environmentalRisk = tempFactor * humidityFactor * soilFactor;

                    // ==================== FIX 1 : riskAdj corrigé ====================
                    // Un score ÉLEVÉ = agriculteur expérimenté = MOINS de risque = prime RÉDUITE
                    // Un score FAIBLE = agriculteur novice = PLUS de risque = prime AUGMENTÉE
                    // Amplitude : ±15% max (score 0 → ×1.25, score 100 → ×0.75)
                    float score = user.getScore() != null ? user.getScore() : 50.0f;

                    BigDecimal riskAdj = BigDecimal.ONE.subtract(
                            BigDecimal.valueOf(score - 50)
                                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(0.30))
                    );
                    // Clamp entre 0.85 et 1.15 pour éviter toute valeur aberrante
                    riskAdj = riskAdj.max(BigDecimal.valueOf(0.85)).min(BigDecimal.valueOf(1.15));
                    // ==================== FIN FIX 1 ====================

                    BigDecimal finalMultiplier = riskAdj.multiply(BigDecimal.valueOf(environmentalRisk));
                    cropPremium = cropPremium.multiply(finalMultiplier).setScale(2, RoundingMode.HALF_UP);

                    formulaInsured = formulaInsured.add(insuredAmount);
                    formulaPremium = formulaPremium.add(cropPremium);

                    // On accumule le risque env. une seule fois (sur STANDARD pour éviter le triple comptage)
                    if ("STANDARD".equals(formula)) {
                        avgEnvRisk += environmentalRisk;
                        cropCount++;
                    }
                }

                details.put(formula, config.toBuilder()
                        .insuredAmount(formulaInsured)
                        .premiumAmount(formulaPremium)
                        .build());

                if (formula.equals(selectedFormula)) {
                    totalPremium = formulaPremium;
                }
            }

            double finalEnvRisk = (cropCount > 0) ? avgEnvRisk / cropCount : 1.0;

            // ==================== Appel IA pour ajustement ====================
            IAIRiskAssessmentService.AIRiskAssessmentResult aiResult = getAiRiskResult(user, selectedCoverType);

            // Plafonner l'ajustement IA entre 0.85 et 1.15 (±15% max)
            double adjustment = Math.max(0.85, Math.min(1.15, aiResult.recommendedAdjustment()));

            BigDecimal finalPremium = totalPremium
                    .multiply(BigDecimal.valueOf(adjustment))
                    .setScale(2, RoundingMode.HALF_UP);

            // ==================== Formule suggérée ====================
            float score = user.getScore() != null ? user.getScore() : 50.0f;
            String aiRiskLevel = aiResult.riskLevel() != null ? aiResult.riskLevel() : "MEDIUM";

            String suggested;
            if (score <= 55 || "HIGH".equals(aiRiskLevel) || "VERY_HIGH".equals(aiRiskLevel) || finalEnvRisk > 1.20) {
                suggested = "PREMIUM";
            } else if (score >= 75 && "LOW".equals(aiRiskLevel) && finalEnvRisk <= 1.10) {
                suggested = "BASIC";
            } else {
                suggested = "STANDARD";
            }

            String recommendationReason = buildRecommendationReason(suggested, user, finalEnvRisk, aiRiskLevel);

            BigDecimal selectedInsured  = details.get(selectedFormula).getInsuredAmount();
            BigDecimal suggestedInsured = details.get(suggested).getInsuredAmount();

            return PremiumEstimationResponse.builder()
                    .totalPremium(finalPremium)
                    .detailsByFormula(details)
                    .suggestedFormula(suggested)
                    .recommendationReason(recommendationReason)
                    .suggestedInsuredAmount(suggestedInsured)
                    .minAllowedInsuredAmount(selectedInsured.multiply(BigDecimal.valueOf(0.8)).setScale(2, RoundingMode.HALF_UP))
                    .maxAllowedInsuredAmount(selectedInsured.multiply(BigDecimal.valueOf(1.2)).setScale(2, RoundingMode.HALF_UP))
                    .aiRiskScore(aiResult.riskScore())
                    .riskLevel(aiRiskLevel)
                    .riskFactors(aiResult.keyRiskFactors())
                    .aiInsights(aiResult.detailedInsights())
                    .build();
        }

        // ==================== IA helpers ====================

        private IAIRiskAssessmentService.AIRiskAssessmentResult getAiRiskResult(User user, CoverageType coverType) {
            if (aiRiskService == null) {
                log.warn("Service IA non injecté → fallback");
                return createFallback();
            }

            try {
                log.info("Appel IA risque pour user {}", user.getId());

                int currentYear = LocalDate.now().getYear();
                double avgEnvRisk = 0.0;
                int cropCount = 0;

                for (Crop crop : user.getCrops()) {
                    CropReference ref = cropReferenceRepository
                            .findByCropTypeAndReferenceYear(crop.getCropType(), currentYear)
                            .orElseGet(() -> cropReferenceRepository
                                    .findTopByCropTypeOrderByReferenceYearDesc(crop.getCropType())
                                    .orElseThrow(() -> new RuntimeException("Aucune référence pour " + crop.getCropType())));

                    double tempFactor     = calculateTemperatureRiskFactor(crop);
                    double humidityFactor = calculateHumidityRiskFactor(crop);
                    double soilFactor     = calculateSoilRiskFactor(crop);
                    avgEnvRisk += tempFactor * humidityFactor * soilFactor;
                    cropCount++;
                }

                double finalEnvRisk = (cropCount > 0) ? avgEnvRisk / cropCount : 1.0;
                float score = user.getScore() != null ? user.getScore() : 50.0f;

                IAIRiskAssessmentService.AIRiskAssessmentResult result =
                        aiRiskService.assessRisk(user.getId(), coverType, finalEnvRisk, (int) score);

                log.info("IA réussie → RiskScore={}, Adjustment={}", result.riskScore(), result.recommendedAdjustment());
                return result;

            } catch (Exception e) {
                log.error("ÉCHEC IA - Cause probable : Ollama indisponible, clé invalide ou problème réseau", e);
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