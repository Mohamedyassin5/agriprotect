package tn.esprit.agri.DTO.DemandeDTO;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import tn.esprit.agri.entities.enums.DecisionCredit;

@Getter
@Setter
public class DecisionFinaleDto {
    @NotNull
    private DecisionCredit decision;

    private String commentaire;
    private String actorId;
}
