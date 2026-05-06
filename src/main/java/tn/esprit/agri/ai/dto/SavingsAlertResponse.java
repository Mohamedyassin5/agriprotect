package tn.esprit.agri.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SavingsAlertResponse {
    @JsonProperty("alertStatus")
    private String alertStatus;
    private double confidence;
    private Map<String, Double> probabilities;
    @JsonProperty("recommendedSavings")
    private double recommendedSavings;
    @JsonProperty("savingCapacity")
    private double savingCapacity;
    @JsonProperty("capacityRatio")
    private double capacityRatio;
    private String interpretation;
    @JsonProperty("alertColor")
    private String alertColor;
}
