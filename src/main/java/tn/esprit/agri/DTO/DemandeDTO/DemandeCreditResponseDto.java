package tn.esprit.agri.DTO.DemandeDTO;

import lombok.*;
import tn.esprit.agri.entities.enums.StatutDemande;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandeCreditResponseDto {

    private Long id;
    private LocalDate dateDemande;
    private StatutDemande statut;
    private Long agriculteurId;
    private Double montantDemande;
    private String description;
}