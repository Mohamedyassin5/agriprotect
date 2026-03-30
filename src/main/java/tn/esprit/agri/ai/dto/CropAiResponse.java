package tn.esprit.agri.ai.dto;

public record CropAiResponse(
        String recommended_crop,
        String model_used
) {}
