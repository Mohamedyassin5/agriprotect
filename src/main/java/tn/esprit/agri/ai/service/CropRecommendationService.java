package tn.esprit.agri.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tn.esprit.agri.ai.client.CropAiClient;
import tn.esprit.agri.ai.dto.CropAiRequest;
import tn.esprit.agri.ai.dto.CropAiResponse;

@Service @RequiredArgsConstructor
public class CropRecommendationService {

    private final CropAiClient cropAiClient;

    @Value("${agri.ai.model:xgb}")
    private String defaultModel;

    public CropAiResponse recommend(CropAiRequest req) {
        String model = (req.model() == null || req.model().isBlank()) ? defaultModel : req.model();

        CropAiRequest normalized = new CropAiRequest(
                req.N(), req.P(), req.K(),
                req.temperature(), req.humidity(), req.ph(),
                req.rainfall(), req.Soil_Fertility_Index(),
                model
        );

        return cropAiClient.predict(normalized);
    }
}
