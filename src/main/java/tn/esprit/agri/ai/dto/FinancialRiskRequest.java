package tn.esprit.agri.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FinancialRiskRequest {
    private double revenue;
    private double expenses;
    @JsonProperty("debtToEquity")
    private double debtToEquity;
    @JsonProperty("netProfit")
    private double netProfit;
    @JsonProperty("avgTemperature")
    private double avgTemperature = 28.0;
    private double rainfall = 400.0;
    @JsonProperty("droughtIndex")
    private double droughtIndex = 3.0;
    @JsonProperty("floodRiskScore")
    private double floodRiskScore = 2.0;
}
