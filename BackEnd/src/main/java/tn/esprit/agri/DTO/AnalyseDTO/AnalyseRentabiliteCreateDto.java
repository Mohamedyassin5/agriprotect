package tn.esprit.agri.DTO.AnalyseDTO;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.esprit.agri.entities.enums.DecisionCredit;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyseRentabiliteCreateDto {

    @NotNull
    @Min(0)
    private Double revenuBrut;

    @NotNull
    @Min(0)
    private Double coutTotal;

    @NotNull
    private DecisionCredit decision;

    private String commentaire;

}