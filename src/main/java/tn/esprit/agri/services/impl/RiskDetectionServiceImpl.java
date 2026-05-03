package tn.esprit.agri.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.agri.ai.dto.WeatherData;
import tn.esprit.agri.entities.Crop;
import tn.esprit.agri.entities.Risque;
import tn.esprit.agri.entities.enums.RiskType;
import tn.esprit.agri.entities.enums.Severity;
import tn.esprit.agri.repositories.CropRepository;
import tn.esprit.agri.repositories.RisqueRepository;
import tn.esprit.agri.services.IWeatherService;
import tn.esprit.agri.services.IRiskDetectionService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RiskDetectionServiceImpl implements IRiskDetectionService {

    private final RisqueRepository risqueRepository;
    private final CropRepository cropRepository;
    private final IWeatherService weatherService;

    @Override
    public List<Risque> detectRisks(Crop crop) {
        log.info("Detecting risks for crop: {} ({})", crop.getId(), crop.getCropType());
        
        List<Risque> detected = new ArrayList<>();
        
        try {
            WeatherData weather = weatherService.getCurrentWeatherByCrop(crop);
            
            // Vérifier température maximale
            if (weather.getTemperature() != null && crop.getMaxTemperature() != null) {
                if (weather.getTemperature() > crop.getMaxTemperature()) {
                    Risque risque = createRisque(
                            crop,
                            RiskType.TEMPERATURE_EXCEEDS_MAX,
                            weather.getTemperature(),
                            crop.getMaxTemperature(),
                            null,
                            Severity.HIGH,
                            "Température dépasse le maximum: " + weather.getTemperature() + "°C > " + crop.getMaxTemperature() + "°C"
                    );
                    detected.add(risque);
                }
            }
            
            // Vérifier température minimale
            if (weather.getTemperature() != null && crop.getMinTemperature() != null) {
                if (weather.getTemperature() < crop.getMinTemperature()) {
                    Risque risque = createRisque(
                            crop,
                            RiskType.TEMPERATURE_BELOW_MIN,
                            weather.getTemperature(),
                            null,
                            crop.getMinTemperature(),
                            Severity.HIGH,
                            "Température en dessous du minimum: " + weather.getTemperature() + "°C < " + crop.getMinTemperature() + "°C"
                    );
                    detected.add(risque);
                }
            }
            
            // Vérifier humidité maximale
            if (weather.getHumidity() != null && crop.getMaxHumidity() != null) {
                if (weather.getHumidity() > crop.getMaxHumidity()) {
                    Risque risque = createRisque(
                            crop,
                            RiskType.HUMIDITY_EXCEEDS_MAX,
                            weather.getHumidity(),
                            crop.getMaxHumidity(),
                            null,
                            Severity.MEDIUM,
                            "Humidité dépasse le maximum: " + weather.getHumidity() + "% > " + crop.getMaxHumidity() + "%"
                    );
                    detected.add(risque);
                }
            }
            
            // Vérifier humidité minimale
            if (weather.getHumidity() != null && crop.getMinHumidity() != null) {
                if (weather.getHumidity() < crop.getMinHumidity()) {
                    Risque risque = createRisque(
                            crop,
                            RiskType.HUMIDITY_BELOW_MIN,
                            weather.getHumidity(),
                            null,
                            crop.getMinHumidity(),
                            Severity.MEDIUM,
                            "Humidité en dessous du minimum: " + weather.getHumidity() + "% < " + crop.getMinHumidity() + "%"
                    );
                    detected.add(risque);
                }
            }
            
        } catch (Exception e) {
            log.error("Error detecting risks for crop {}: ", crop.getId(), e);
        }
        
        return detected;
    }

    @Override
    public List<Risque> detectAllCropsRisks() {
        log.info("Running scheduled risk detection for all crops");
        List<Crop> allCrops = cropRepository.findAll();
        List<Risque> allDetected = new ArrayList<>();
        
        for (Crop crop : allCrops) {
            List<Risque> detected = detectRisks(crop);
            allDetected.addAll(detected);
        }
        
        return allDetected;
    }

    @Override
    public void checkAndCreateSinistre(Crop crop) {
        List<Risque> risks = detectRisks(crop);
        
        for (Risque risk : risks) {
            // Vérifier si un risque similaire non résolu existe déjà
            boolean alreadyExists = risqueRepository.findUnresolvedByCropId(crop.getId())
                    .stream()
                    .anyMatch(r -> r.getTypeSinistre() == risk.getTypeSinistre());
            
            if (!alreadyExists) {
                // Sauvegarder directement sans vérification de quota (la logique de quota météo est supprimée)
                risqueRepository.save(risk);
                log.info("Created risque: {} for crop: {}", risk.getTypeSinistre(), crop.getId());
            }
        }
    }

    @Override
    public void resolveSinistre(String risqueId) {
        risqueRepository.findById(risqueId).ifPresent(risque -> {
            risque.setIsResolved(true);
            risque.setResolvedAt(LocalDateTime.now());
            risqueRepository.save(risque);
            log.info("Resolved risque: {}", risqueId);
        });
    }

    private Risque createRisque(Crop crop, RiskType type, Float currentValue,
                                     Float maxAllowed, Float minAllowed, Severity severity, String description) {
        return Risque.builder()
                .crop(crop)
                .user(crop.getUser())
                .typeSinistre(type)
                .currentValue(currentValue)
                .maxAllowed(maxAllowed)
                .minAllowed(minAllowed)
                .severity(severity)
                .description(description)
                .isResolved(false)
                .build();
    }
}

