package tn.esprit.agri.DTO.DemandeDTO;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
public class UpdateDemandeCreditDto {

    private LocalDate dateDemande;

    @Min(value = 100, message = "Le montant doit être supérieur ou égal à 100")
    private Double montantDemande;

    @NotBlank(message = "La description ne peut pas être vide")
    private String description;

}