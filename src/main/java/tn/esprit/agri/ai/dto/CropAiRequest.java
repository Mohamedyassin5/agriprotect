package tn.esprit.agri.ai.dto;

import jakarta.validation.constraints.NotNull;

public record CropAiRequest(
        @NotNull Double N,
        @NotNull Double P,
        @NotNull Double K,
        @NotNull Double temperature,
        @NotNull Double humidity,
        @NotNull Double ph,
        @NotNull Double rainfall,
        @NotNull Double Soil_Fertility_Index,
        String model,
        Integer top_k
) {}
