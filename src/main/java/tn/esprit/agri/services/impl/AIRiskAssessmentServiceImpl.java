package tn.esprit.agri.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.agri.DTO.PremiumEstimationResponse;
import tn.esprit.agri.entities.Crop;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.entities.enums.CoverageType;
import tn.esprit.agri.repositories.UserRepository;
import tn.esprit.agri.services.IAIRiskAssessmentService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AIRiskAssessmentServiceImpl implements IAIRiskAssessmentService {

    private final OllamaRestClient ollamaClient;
    private final UserRepository userRepository;

    @Override
    public AICompleteEstimationResult calculateCompleteEstimation(String userId, CoverageType coverType) {
        try {
            // Charger l'utilisateur avec ses cultures
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé: " + userId));

            log.info("Cultures chargées: {}", user.getCrops().size());

            if (!ollamaClient.isRunning()) {
                log.warn("Ollama n'est pas disponible → fallback Java");
                return createJavaFallbackEstimation(user, coverType);
            }

            log.info("Appel IA complète pour user: {}, coverType: {}", userId, coverType);

            String prompt = buildCompleteEstimationPrompt(user, coverType);
            String aiResponse = ollamaClient.generate(prompt);

            log.info("Réponse IA complète reçue ({} caractères)", aiResponse.length());
            log.debug("Réponse brute IA: {}", aiResponse);

            return parseCompleteEstimation(aiResponse, user, coverType);

        } catch (Exception e) {
            log.error("Erreur lors de l'évaluation IA complète", e);
            User user = userRepository.findById(userId).orElse(null);
            return createJavaFallbackEstimation(user, coverType);
        }
    }

    @Override
    public AIRiskAssessmentResult assessRisk(String userId, CoverageType coverType) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!ollamaClient.isRunning()) {
                log.warn("Ollama n'est pas disponible → fallback");
                return createFallbackAssessment(user, coverType);
            }

            log.info("Appel IA risque pour user: {}, coverType: {}", userId, coverType);

            String prompt = buildRiskAssessmentPrompt(user, coverType);
            String aiResponse = ollamaClient.generate(prompt);

            log.info("Réponse IA risque reçue ({} caractères)", aiResponse.length());

            return parseRiskAssessment(aiResponse, user, coverType);

        } catch (Exception e) {
            log.error("Erreur lors de l'évaluation du risque IA", e);
            User user = userRepository.findById(userId).orElse(null);
            return createFallbackAssessment(user, coverType);
        }
    }

    // ====================== PROMPTS ======================

    private String buildRiskAssessmentPrompt(User user, CoverageType coverType) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Tu es un expert en assurance agricole tunisienne. Évalue le risque global.\n\n");

        prompt.append("Informations agriculteur:\n");
        prompt.append("- Score d'expérience: ").append(user.getScore() != null ? user.getScore() : 50).append("/100\n");
        prompt.append("- Nombre de cultures: ").append(user.getCrops().size()).append("\n\n");

        prompt.append("Cultures:\n");
        for (Crop crop : user.getCrops()) {
            prompt.append("- ").append(crop.getCropType())
                    .append(" | Surface: ").append(String.format("%.2f", crop.getSurface()))
                    .append(" ha | Sol: ").append(crop.getTypeterres() != null ? crop.getTypeterres() : "Standard")
                    .append("\n");
        }

        prompt.append("\nCouverture demandée: ").append(coverType).append("\n\n");

        prompt.append("Réponds UNIQUEMENT avec ce JSON valide (aucun texte supplémentaire) :\n");
        prompt.append("""
        {
          "riskScore": 0.XX,
          "riskLevel": "LOW" ou "MEDIUM" ou "HIGH",
          "recommendedAdjustment": 1.XX,
          "keyRiskFactors": ["facteur1", "facteur2"],
          "insights": "Commentaire court en français"
        }
        """);
        prompt.append("Commence directement par { et termine par }.\nJSON:");

        return prompt.toString();
    }

    private String buildCompleteEstimationPrompt(User user, CoverageType coverType) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Tu es un expert en assurance agricole tunisienne. Calcule les primes d'assurance de manière réaliste et juste.\n\n");

        prompt.append("INFORMATIONS AGRICULTEUR:\n");
        prompt.append("- Score d'expérience: ").append(user.getScore() != null ? user.getScore() : 50).append("/100\n");
        prompt.append("- Cultures déclarées: ").append(user.getCrops().size()).append("\n\n");

        prompt.append("DÉTAIL DES CULTURES:\n");
        for (Crop crop : user.getCrops()) {
            prompt.append("- ").append(crop.getCropType())
                    .append(" | Surface: ").append(crop.getSurface()).append(" ha")
                    .append(" | Type de sol: ").append(crop.getTypeterres() != null ? crop.getTypeterres() : "Standard")
                    .append("\n");
        }

        prompt.append("\nType de couverture demandé: ").append(coverType).append("\n\n");

        prompt.append("Réponds EXCLUSIVEMENT avec un JSON valide. Aucun texte avant ou après le JSON.\n");
        prompt.append("Utilise EXACTEMENT cette structure :\n");
        prompt.append("""
        {
          "formulas": {
            "BASIC": {"insuredAmount": nombre, "premiumAmount": nombre},
            "STANDARD": {"insuredAmount": nombre, "premiumAmount": nombre},
            "PREMIUM": {"insuredAmount": nombre, "premiumAmount": nombre}
          },
          "suggestedFormula": "BASIC" ou "STANDARD" ou "PREMIUM",
          "riskScore": nombre entre 0.0 et 1.0,
          "riskLevel": "LOW" ou "MEDIUM" ou "HIGH",
          "riskFactors": ["facteur1", "facteur2"],
          "insights": "Courte analyse en français (1-2 phrases)"
        }
        """);
        prompt.append("Commence directement par { et termine par }. Pas de ```json ni aucun commentaire.\n");
        prompt.append("JSON:");

        return prompt.toString();
    }

    // ====================== PARSING ======================

    private AIRiskAssessmentResult parseRiskAssessment(String aiResponse, User user, CoverageType coverType) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> jsonResponse = mapper.readValue(aiResponse, Map.class);

            double riskScore = ((Number) jsonResponse.getOrDefault("riskScore", 0.5)).doubleValue();
            String riskLevel = (String) jsonResponse.getOrDefault("riskLevel", "MEDIUM");
            double recommendedAdjustment = ((Number) jsonResponse.getOrDefault("recommendedAdjustment", 1.0)).doubleValue();

            @SuppressWarnings("unchecked")
            List<String> keyRiskFactors = (List<String>) jsonResponse.getOrDefault("keyRiskFactors", new ArrayList<>());

            Map<String, Object> detailedInsights = new HashMap<>();
            detailedInsights.put("insights", jsonResponse.getOrDefault("insights", "Analyse IA terminée"));
            detailedInsights.put("coverType", coverType.name());
            detailedInsights.put("mode", "IA");

            riskScore = Math.max(0.0, Math.min(1.0, riskScore));
            recommendedAdjustment = Math.max(0.8, Math.min(1.2, recommendedAdjustment));

            return new AIRiskAssessmentResult(riskScore, riskLevel, recommendedAdjustment, keyRiskFactors, detailedInsights);

        } catch (Exception e) {
            log.warn("Erreur parsing réponse risk IA: {}", e.getMessage());
            return createFallbackAssessment(user, coverType);
        }
    }

    private AICompleteEstimationResult parseCompleteEstimation(String aiResponse, User user, CoverageType coverType) {
        try {
            String cleanResponse = cleanJsonResponse(aiResponse);
            log.debug("JSON nettoyé pour parsing: {}", cleanResponse);

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> json = mapper.readValue(cleanResponse, Map.class);

            // Extraction des formules
            @SuppressWarnings("unchecked")
            Map<String, Object> formulasMap = (Map<String, Object>) json.get("formulas");

            if (formulasMap == null) {
                log.warn("Clé 'formulas' manquante dans la réponse IA");
                throw new IllegalArgumentException("Structure JSON invalide");
            }

            Map<String, PremiumEstimationResponse.FormulaDetail> details = new LinkedHashMap<>();

            for (String formula : List.of("BASIC", "STANDARD", "PREMIUM")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> formulaData = (Map<String, Object>) formulasMap.get(formula);

                if (formulaData == null || !formulaData.containsKey("insuredAmount") || !formulaData.containsKey("premiumAmount")) {
                    log.warn("Données manquantes pour la formule {}", formula);
                    // Valeurs par défaut en cas d'erreur sur une formule
                    double defaultInsured = 40000 * getCoveragePercentage(formula);
                    double defaultPremium = defaultInsured * 0.017;

                    details.put(formula, createDefaultFormulaDetail(formula, defaultInsured, defaultPremium));
                    continue;
                }

                double coveragePercentage = getCoveragePercentage(formula);
                double franchisePercentage = getFranchisePercentage(formula);
                String shortDescription = getShortDescription(formula);

                BigDecimal insuredAmount = BigDecimal.valueOf(((Number) formulaData.get("insuredAmount")).doubleValue())
                        .setScale(2, RoundingMode.HALF_UP);

                BigDecimal premiumAmount = BigDecimal.valueOf(((Number) formulaData.get("premiumAmount")).doubleValue())
                        .setScale(2, RoundingMode.HALF_UP);

                details.put(formula, PremiumEstimationResponse.FormulaDetail.builder()
                        .formulaName(formula)
                        .coveragePercentage(BigDecimal.valueOf(coveragePercentage))
                        .franchisePercentage(BigDecimal.valueOf(franchisePercentage))
                        .insuredAmount(insuredAmount)
                        .premiumAmount(premiumAmount)
                        .shortDescription(shortDescription)
                        .build());
            }

            String suggestedFormula = (String) json.getOrDefault("suggestedFormula", "STANDARD");
            double riskScore = ((Number) json.getOrDefault("riskScore", 0.5)).doubleValue();
            String riskLevel = (String) json.getOrDefault("riskLevel", "MEDIUM");

            @SuppressWarnings("unchecked")
            List<String> riskFactors = (List<String>) json.getOrDefault("riskFactors", List.of("Analyse IA"));

            // Gestion robuste de "insights" (peut être String ou Map)
            Object insightsObj = json.get("insights");
            Map<String, Object> insights = new HashMap<>();
            if (insightsObj instanceof Map) {
                insights.putAll((Map<String, Object>) insightsObj);
            } else if (insightsObj instanceof String) {
                insights.put("text", insightsObj);
            }
            insights.putIfAbsent("mode", "IA");

            riskScore = Math.max(0.0, Math.min(1.0, riskScore));

            log.info("Parsing IA réussi → suggestedFormula={}, riskScore={}", suggestedFormula, riskScore);

            return new AICompleteEstimationResult(details, suggestedFormula, riskScore, riskLevel, riskFactors, insights);

        } catch (Exception e) {
            log.error("Erreur parsing IA complète. Réponse brute: {}", aiResponse, e);
            log.warn("Passage en fallback Java");
            return createJavaFallbackEstimation(user, coverType);
        }
    }

    // Méthodes helper pour éviter la duplication
    private double getCoveragePercentage(String formula) {
        return switch (formula) {
            case "BASIC" -> 0.60;
            case "STANDARD" -> 0.75;
            case "PREMIUM" -> 0.90;
            default -> 0.75;
        };
    }

    private double getFranchisePercentage(String formula) {
        return switch (formula) {
            case "BASIC" -> 0.30;
            case "STANDARD" -> 0.20;
            case "PREMIUM" -> 0.10;
            default -> 0.20;
        };
    }

    private String getShortDescription(String formula) {
        return switch (formula) {
            case "BASIC" -> "Min.";
            case "STANDARD" -> "Équilibre";
            case "PREMIUM" -> "Max.";
            default -> "";
        };
    }

    private PremiumEstimationResponse.FormulaDetail createDefaultFormulaDetail(String formula, double insured, double premium) {
        return PremiumEstimationResponse.FormulaDetail.builder()
                .formulaName(formula)
                .coveragePercentage(BigDecimal.valueOf(getCoveragePercentage(formula)))
                .franchisePercentage(BigDecimal.valueOf(getFranchisePercentage(formula)))
                .insuredAmount(BigDecimal.valueOf(insured))
                .premiumAmount(BigDecimal.valueOf(premium))
                .shortDescription(getShortDescription(formula))
                .build();
    }
    /**
     * Nettoyage supplémentaire du JSON
     */
    private String cleanJsonResponse(String response) {
        if (response == null) return "{}";
        String cleaned = response.trim();

        // Supprimer blocs markdown
        cleaned = cleaned.replaceAll("(?i)```\\s*(json)?", "").replaceAll("```", "").trim();

        // Extraire uniquement la partie entre { et }
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');

        if (start != -1 && end != -1 && end > start) {
            cleaned = cleaned.substring(start, end + 1);
        }

        return cleaned;
    }

    // ====================== FALLBACKS (inchangés) ======================

    private AIRiskAssessmentResult createFallbackAssessment(User user, CoverageType coverType) {
        if (user == null) {
            return new AIRiskAssessmentResult(0.5, "MEDIUM", 1.0, List.of(), Map.of("mode", "FALLBACK"));
        }

        List<String> riskFactors = new ArrayList<>();
        double riskScore = 0.5;

        for (Crop crop : user.getCrops()) {
            if (crop.getSurface() > 10) {
                riskScore += 0.1;
                riskFactors.add("Grande surface: " + crop.getSurface() + " ha");
            }
            String type = crop.getCropType().toLowerCase();
            if (type.contains("tomate") || type.contains("fraise")) {
                riskScore += 0.15;
                riskFactors.add("Culture sensible: " + crop.getCropType());
            }
        }

        if (user.getScore() != null) {
            if (user.getScore() < 30) {
                riskScore += 0.2;
                riskFactors.add("Faible expérience agricole");
            } else if (user.getScore() > 70) {
                riskScore -= 0.15;
                riskFactors.add("Bonne expérience agricole");
            }
        }

        riskScore = Math.max(0.0, Math.min(1.0, riskScore));
        String riskLevel = riskScore < 0.33 ? "LOW" : (riskScore < 0.66 ? "MEDIUM" : "HIGH");
        double recommendedAdjustment = Math.max(0.8, Math.min(1.2, 0.9 + (riskScore * 0.3)));

        return new AIRiskAssessmentResult(riskScore, riskLevel, recommendedAdjustment, riskFactors, Map.of("mode", "FALLBACK"));
    }

    private AICompleteEstimationResult createJavaFallbackEstimation(User user, CoverageType coverType) {
        if (user == null) {
            return new AICompleteEstimationResult(Map.of(), "STANDARD", 0.5, "MEDIUM", List.of(), Map.of("mode", "JAVA_FALLBACK"));
        }

        log.info("Utilisation du fallback Java pour l'estimation complète");

        Map<String, PremiumEstimationResponse.FormulaDetail> details = new LinkedHashMap<>();
        double totalSurface = user.getCrops().stream().mapToDouble(Crop::getSurface).sum();

        for (String formula : List.of("BASIC", "STANDARD", "PREMIUM")) {
            double coveragePercentage = switch (formula) {
                case "BASIC" -> 0.60;
                case "STANDARD" -> 0.75;
                case "PREMIUM" -> 0.90;
                default -> 0.75;
            };

            double insuredAmount = totalSurface * 5000 * 2.5 * coveragePercentage;

            double riskMultiplier = 1.0;
            if (user.getScore() != null) {
                riskMultiplier = Math.max(0.8, Math.min(1.2, 1.0 - (user.getScore() - 50) / 100.0 * 0.5));
            }

            double premiumAmount = insuredAmount * 0.02 * riskMultiplier;

            details.put(formula, PremiumEstimationResponse.FormulaDetail.builder()
                    .formulaName(formula)
                    .coveragePercentage(BigDecimal.valueOf(coveragePercentage))
                    .franchisePercentage(BigDecimal.valueOf(switch (formula) {
                        case "BASIC" -> 0.30;
                        case "STANDARD" -> 0.20;
                        case "PREMIUM" -> 0.10;
                        default -> 0.20;
                    }))
                    .insuredAmount(BigDecimal.valueOf(insuredAmount))
                    .premiumAmount(BigDecimal.valueOf(premiumAmount))
                    .shortDescription(switch (formula) {
                        case "BASIC" -> "Min.";
                        case "STANDARD" -> "Équilibre";
                        case "PREMIUM" -> "Max.";
                        default -> "";
                    })
                    .build());
        }

        String suggestedFormula = user.getScore() != null ?
                (user.getScore() <= 55 ? "PREMIUM" : (user.getScore() >= 75 ? "BASIC" : "STANDARD")) : "STANDARD";

        double riskScore = user.getScore() != null ? 1.0 - (user.getScore() / 100.0) : 0.5;
        String riskLevel = riskScore < 0.33 ? "LOW" : (riskScore < 0.66 ? "MEDIUM" : "HIGH");

        return new AICompleteEstimationResult(details, suggestedFormula, riskScore, riskLevel,
                List.of("Fallback Java"), Map.of("mode", "JAVA_FALLBACK"));
    }
}