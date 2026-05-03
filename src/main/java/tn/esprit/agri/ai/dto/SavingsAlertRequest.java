package tn.esprit.agri.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SavingsAlertRequest {
    private double revenue;
    private double expenses;
    @JsonProperty("loanAmount")
    private double loanAmount = 0.0;
    @JsonProperty("droughtIndex")
    private double droughtIndex = 3.0;
    @JsonProperty("floodRiskScore")
    private double floodRiskScore = 2.0;
    @JsonProperty("policySupportScore")
    private double policySupportScore = 5.0;
}
