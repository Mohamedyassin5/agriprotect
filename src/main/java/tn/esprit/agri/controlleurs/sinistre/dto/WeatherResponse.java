package tn.esprit.agri.controlleurs.sinistre.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherResponse {
    private Float temperature;
    private Float humidity;
    private Float windSpeed;
    private Float rainfall;
    private String description;
    private Long timestamp;
}
