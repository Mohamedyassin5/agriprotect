package tn.esprit.agri.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.agri.ai.client.WeatherClient;
import tn.esprit.agri.ai.dto.WeatherData;
import tn.esprit.agri.entities.Crop;
import tn.esprit.agri.services.IWeatherService;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherServiceImpl implements IWeatherService {

    private final WeatherClient weatherClient;

    @Override
    public WeatherData getCurrentWeatherByCrop(Crop crop) {
        // TODO: Ajouter latitude/longitude dans l'entité Crop si nécessaire
        // Par défaut, utiliser une localisation par défaut ou celle de l'utilisateur
        // Pour l'instant, utiliser une localisation par défaut (Tunisie)
        return getCurrentWeatherByCoordinates(36.8065, 10.1686);
    }

    @Override
    public WeatherData getCurrentWeatherByCoordinates(Double latitude, Double longitude) {
        try {
            log.info("Fetching weather for coordinates: lat={}, lon={}", latitude, longitude);
            return weatherClient.getCurrentWeather(latitude, longitude);
        } catch (Exception e) {
            log.error("Error fetching weather: ", e);
            throw new RuntimeException("Failed to fetch weather data", e);
        }
    }
}
