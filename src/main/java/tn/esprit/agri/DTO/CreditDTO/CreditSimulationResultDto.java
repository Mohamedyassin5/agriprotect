package tn.esprit.agri.DTO.CreditDTO;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreditSimulationResultDto {
    private Double montant;
    private Double tauxInteret;
    private Integer dureeMois;
    private Double mensualite;
    private Double totalInterets;
    private Double totalCost;
    private Double affordabilityRatio;
}
