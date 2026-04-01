package tn.esprit.agri.DTO.DemandeDTO;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
public class UpdateDemandeCreditDto {

    private LocalDate dateDemande;

    @Min(value = 100, message = "Le montant doit être supérieur ou égal à 100")
    private Double montantDemande;

    @Size(max = 500, message = "La description ne doit pas dépasser 500 caractères")
    private String description;

}