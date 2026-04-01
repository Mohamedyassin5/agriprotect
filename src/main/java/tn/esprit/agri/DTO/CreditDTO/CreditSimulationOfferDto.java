package tn.esprit.agri.DTO.CreditDTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreditSimulationOfferDto {
    @NotNull
    @Positive
    private Double montant;

    @NotNull
    @DecimalMin("0.0")
    private Double tauxInteret;

    @NotNull
    @Min(1)
    private Integer dureeMois;

    @DecimalMin("0.0")
    private Double frais = 0.0;
}
