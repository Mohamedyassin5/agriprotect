package tn.esprit.agri.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherData {
    private Float temperature;
    private Float humidity;
    private Float rainfall;
    private String weatherDescription;
    private Float windSpeed;
    private String location;
    private Long timestamp;
}
