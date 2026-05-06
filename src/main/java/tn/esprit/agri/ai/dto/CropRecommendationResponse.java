package tn.esprit.agri.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CropRecommendationResponse {

    private List<String> recommended_crops;
    private String model_used;
    private ParameterAnalysis parameterAnalysis;
    private List<CropInsight> cropInsights;
    private List<String> actionPlan;
    private String soilAssessment;

    @Data
    @Builder
    public static class ParameterAnalysis {
        private ParameterStatus nitrogen;
        private ParameterStatus phosphorus;
        private ParameterStatus potassium;
        private ParameterStatus temperature;
        private ParameterStatus humidity;
        private ParameterStatus ph;
        private ParameterStatus rainfall;
        private ParameterStatus soilFertility;
    }

    @Data
    @Builder
    public static class ParameterStatus {
        private String name;
        private double value;
        private String unit;
        private String status;       // OPTIMAL / BON / ACCEPTABLE / FAIBLE / CRITIQUE
        private String assessment;   // Explication concrète
        private String suggestion;   // Action corrective si nécessaire
    }

    @Data
    @Builder
    public static class CropInsight {
        private String cropEn;
        private String cropFr;
        private String matchLevel;            // EXCELLENT / BON / MOYEN
        private List<String> matchingFactors;  // Pourquoi ce crop correspond
        private List<String> limitingFactors;  // Ce qui pourrait limiter le rendement
        private String plantingPeriod;         // Période de semis en Tunisie
        private String estimatedYieldPerHa;    // Rendement estimé
        private String careAdvice;             // Conseil de culture spécifique
    }
}
