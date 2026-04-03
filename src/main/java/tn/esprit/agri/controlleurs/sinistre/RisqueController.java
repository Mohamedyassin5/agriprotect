package tn.esprit.agri.controlleurs.sinistre;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.esprit.agri.controlleurs.sinistre.dto.RiskCheckResponse;
import tn.esprit.agri.controlleurs.sinistre.dto.RisqueResponse;
import tn.esprit.agri.controlleurs.sinistre.dto.WeatherResponse;
import tn.esprit.agri.ai.dto.WeatherData;

import tn.esprit.agri.entities.Crop;
import tn.esprit.agri.entities.Risque;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.repositories.CropRepository;
import tn.esprit.agri.repositories.RisqueRepository;
import tn.esprit.agri.repositories.UserRepository;

import tn.esprit.agri.services.IRiskDetectionService;
import tn.esprit.agri.services.IWeatherService;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/agri/risques")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class RisqueController {

    private final IRiskDetectionService riskDetectionService;
    private final IWeatherService weatherService;
    private final RisqueRepository risqueRepository;
    private final CropRepository cropRepository;
    private final UserRepository userRepository;

    /**
     * Déclencher la détection des risques pour une culture spécifique
     */
    @PostMapping("/check/{cropId}")
    public ResponseEntity<?> checkRisksForCrop(
            @PathVariable String cropId,
            Authentication authentication
    ) {
        try {
            Crop crop = cropRepository.findById(cropId)
                    .orElseThrow(() -> new RuntimeException("Crop not found"));

            // 1. Déclencher la détection
            riskDetectionService.checkAndCreateSinistre(crop);

            // 2. Récupérer la météo actuelle pour l'inclure dans la réponse
            WeatherData weather = weatherService.getCurrentWeatherByCrop(crop);
            WeatherResponse weatherResp = new WeatherResponse(
                    weather.getTemperature(),
                    weather.getHumidity(),
                    weather.getWindSpeed(),
                    weather.getRainfall(),
                    weather.getWeatherDescription(),
                    weather.getTimestamp()
            );

            // 3. Récupérer les risques non résolus existants
            List<Risque> unresolved = risqueRepository.findUnresolvedByCropId(cropId);
            List<RisqueResponse> riskResponses = unresolved.stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());

            // 4. Construire la réponse complète
            RiskCheckResponse response = RiskCheckResponse.builder()
                    .statusMessage(riskResponses.isEmpty() ? "Météo favorable, aucun risque détecté." : "Risques détectés pour cette culture.")
                    .currentWeather(weatherResp)
                    .detectedRisks(riskResponses)
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error checking risks: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }


    /**
     * Récupérer tous les risques pour une culture
     */
    @GetMapping("/crop/{cropId}")
    public ResponseEntity<List<RisqueResponse>> getRisquesByCrop(@PathVariable String cropId) {
        List<Risque> risques = risqueRepository.findByCropId(cropId);
        List<RisqueResponse> responses = risques.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    /**
     * Récupérer tous les risques de l'utilisateur connecté
     */
    @GetMapping("/my-risques")
    public ResponseEntity<List<RisqueResponse>> getMyUnresolvedRisques(
            Authentication authentication
    ) {
        try {
            User user = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<Risque> risques = risqueRepository.findUnresolvedByUserId(user.getId());
            List<RisqueResponse> responses = risques.stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * Récupérer un risque par ID
     */
    @GetMapping("/{risqueId}")
    public ResponseEntity<RisqueResponse> getRisqueById(@PathVariable String risqueId) {
        return risqueRepository.findById(risqueId)
                .map(risque -> ResponseEntity.ok(toResponse(risque)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Marquer un risque comme résolu
     */
    @PutMapping("/{risqueId}/resolve")
    public ResponseEntity<RisqueResponse> resolveRisque(@PathVariable String risqueId) {
        try {
            riskDetectionService.resolveSinistre(risqueId);
            Risque resolved = risqueRepository.findById(risqueId)
                    .orElseThrow(() -> new RuntimeException("Risque not found"));
            return ResponseEntity.ok(toResponse(resolved));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    /**
     * Récupérer les risques non résolus
     */
    @GetMapping("/unresolved/all")
    public ResponseEntity<List<RisqueResponse>> getUnresolvedRisques() {
        List<Risque> risques = risqueRepository.findByIsResolved(false);
        List<RisqueResponse> responses = risques.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }


    /**
     * Récupérer la météo actuelle pour une culture
     */
    @GetMapping("/weather/crop/{cropId}")
    public ResponseEntity<?> getWeatherForCrop(@PathVariable String cropId) {
        try {
            Crop crop = cropRepository.findById(cropId)
                    .orElseThrow(() -> new RuntimeException("Crop not found"));

            WeatherData weather = weatherService.getCurrentWeatherByCrop(crop);
            WeatherResponse response = new WeatherResponse(
                    weather.getTemperature(),
                    weather.getHumidity(),
                    weather.getWindSpeed(),
                    weather.getRainfall(),
                    weather.getWeatherDescription(),
                    weather.getTimestamp()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching weather: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // Mapper
    private RisqueResponse toResponse(Risque risque) {
        RisqueResponse response = new RisqueResponse();
        response.setId(risque.getId());
        response.setCropId(risque.getCrop().getId());
        response.setCropType(risque.getCrop().getCropType());
        response.setUserId(risque.getUser().getId());
        response.setUserEmail(risque.getUser().getEmail());
        response.setRiskType(risque.getTypeSinistre());
        response.setDescription(risque.getDescription());
        response.setCurrentValue(risque.getCurrentValue());
        response.setMaxAllowed(risque.getMaxAllowed());
        response.setMinAllowed(risque.getMinAllowed());
        response.setSeverity(risque.getSeverity());
        response.setDetectedAt(risque.getDetectedAt());
        response.setResolvedAt(risque.getResolvedAt());
        response.setIsResolved(risque.getIsResolved());
        return response;
    }

}
