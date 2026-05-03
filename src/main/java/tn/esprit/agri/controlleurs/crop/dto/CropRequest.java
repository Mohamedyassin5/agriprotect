package tn.esprit.agri.controlleurs.crop.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CropRequest {

    @NotBlank(message = "cropType is required")
    private String cropType;

    @NotNull(message = "surface is required")
    @Positive(message = "surface must be > 0")
    private Float surface;

    @NotNull(message = "optimalHumidity is required")
    @Min(0) @Max(100)
    private Float optimalHumidity;

    @NotNull(message = "minHumidity is required")
    @Min(0) @Max(100)
    private Float minHumidity;

    @NotNull(message = "maxHumidity is required")
    @Min(0) @Max(100)
    private Float maxHumidity;

    @NotNull(message = "minTemperature is required")
    private Float minTemperature;

    @NotNull(message = "maxTemperature is required")
    private Float maxTemperature;

    @NotNull(message = "averageTemperature is required")
    private Float averageTemperature;

    @NotNull(message = "startDate is required")
    private LocalDate startDate;

    @NotNull(message = "endDate is required")
    private LocalDate endDate;

    @NotBlank(message = "typeterres is required")
    private String typeterres;
}
