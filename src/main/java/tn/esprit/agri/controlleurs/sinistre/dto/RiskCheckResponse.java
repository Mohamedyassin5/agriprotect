package tn.esprit.agri.controlleurs.sinistre.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskCheckResponse {
    private String statusMessage;
    private WeatherResponse currentWeather;
    private List<RisqueResponse> detectedRisks;
}
