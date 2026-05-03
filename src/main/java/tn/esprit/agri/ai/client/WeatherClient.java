package tn.esprit.agri.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import tn.esprit.agri.ai.dto.WeatherData;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherClient {

    private final WebClient.Builder builder;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String OPEN_METEO_URL = "https://api.open-meteo.com/v1";

    /**
     * Récupère les données météo actuelles via Open-Meteo (API gratuite, sans clé)
     * @param latitude Latitude du lieu
     * @param longitude Longitude du lieu
     * @return WeatherData avec température, humidité, etc.
     */
    public WeatherData getCurrentWeather(Double latitude, Double longitude) {
        try {
            String response = builder.baseUrl(OPEN_METEO_URL)
                    .build()
                    .get()
                    .uri(uri -> uri.path("/forecast")
                            .queryParam("latitude", latitude)
                            .queryParam("longitude", longitude)
                            .queryParam("current", "temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m")
                            .queryParam("timezone", "auto")
                            .build()
                    )
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            return parseOpenMeteoResponse(response);

        } catch (Exception e) {
            log.error("Error fetching weather from Open-Meteo: ", e);
            throw new RuntimeException("Failed to fetch weather data: " + e.getMessage());
        }
    }

    /**
     * Parse la réponse JSON de Open-Meteo correctement avec Jackson
     */
    private WeatherData parseOpenMeteoResponse(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode current = root.get("current");

            if (current == null) {
                throw new RuntimeException("No 'current' data in response");
            }

            WeatherData data = new WeatherData();

            // Extraire les données de façon sûre avec Jackson
            if (current.has("temperature_2m")) {
                data.setTemperature(current.get("temperature_2m").floatValue());
            }

            if (current.has("relative_humidity_2m")) {
                data.setHumidity(current.get("relative_humidity_2m").floatValue());
            }

            if (current.has("wind_speed_10m")) {
                data.setWindSpeed(current.get("wind_speed_10m").floatValue());
            }

            if (current.has("precipitation")) {
                data.setRainfall(current.get("precipitation").floatValue());
            } else {
                data.setRainfall(0f);
            }

            if (current.has("weather_code")) {
                data.setWeatherDescription("Weather code: " + current.get("weather_code").asInt());
            } else {
                data.setWeatherDescription("Open-Meteo Data");
            }

            data.setTimestamp(System.currentTimeMillis());

            log.info("Weather data parsed successfully: temp={}, humidity={}, wind={}", 
                    data.getTemperature(), data.getHumidity(), data.getWindSpeed());

            return data;

        } catch (Exception e) {
            log.error("Error parsing weather response: ", e);
            throw new RuntimeException("Failed to parse weather data: " + e.getMessage());
        }
    }
}
