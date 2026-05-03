package tn.esprit.agri.services;

import tn.esprit.agri.ai.dto.WeatherData;
import tn.esprit.agri.entities.Crop;



public interface IWeatherService {
    WeatherData getCurrentWeatherByCrop(Crop crop);
    WeatherData getCurrentWeatherByCoordinates(Double latitude, Double longitude);
}
