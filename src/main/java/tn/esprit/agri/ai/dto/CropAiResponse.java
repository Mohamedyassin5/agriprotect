package tn.esprit.agri.ai.dto;

import java.util.List;

public record CropAiResponse(
        List<String> recommended_crops,
        String model_used
) {}
