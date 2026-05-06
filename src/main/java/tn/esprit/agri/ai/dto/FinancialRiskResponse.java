package tn.esprit.agri.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FinancialRiskResponse {
    @JsonProperty("riskLevel")
    private String riskLevel;
    private double confidence;
    private Map<String, Double> probabilities;
    private String interpretation;
    @JsonProperty("alertColor")
    private String alertColor;
}
