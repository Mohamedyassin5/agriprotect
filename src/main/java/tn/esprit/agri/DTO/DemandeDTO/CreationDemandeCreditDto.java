package tn.esprit.agri.DTO.DemandeDTO;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreationDemandeCreditDto {

    @NotNull(message = "La date de demande est obligatoire")
    @PastOrPresent
    private LocalDate dateDemande;

    @NotNull(message = "L'agriculteur est obligatoire")
    @Min(1)
    private Long agriculteurId;

    @NotNull(message = "Le montant demandé est obligatoire")
    @Positive(message = "Le montant doit être positif")
    private Double montantDemande;

    @Size(max = 500)
    private String description;
}