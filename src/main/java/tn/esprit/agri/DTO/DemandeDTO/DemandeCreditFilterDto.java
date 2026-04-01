package tn.esprit.agri.DTO.DemandeDTO;

import lombok.Getter;
import lombok.Setter;
import tn.esprit.agri.entities.enums.StatutDemande;

import java.time.LocalDate;

@Getter
@Setter
public class DemandeCreditFilterDto {
    private Integer page = 0;
    private Integer size = 20;
    private String sortBy = "id";
    private String direction = "desc";
    private StatutDemande statut;
    private LocalDate dateFrom;
    private LocalDate dateTo;
}
