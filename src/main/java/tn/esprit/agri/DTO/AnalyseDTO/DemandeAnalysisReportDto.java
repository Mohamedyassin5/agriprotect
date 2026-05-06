package tn.esprit.agri.DTO.AnalyseDTO;

import lombok.Builder;
import lombok.Getter;
import tn.esprit.agri.entities.enums.StatutDemande;

@Getter
@Builder
public class DemandeAnalysisReportDto {
    private Long demandeId;
    private String agriculteurId;
    private StatutDemande statut;
    private Double montantDemande;
    private Double revenuBrut;
    private Double coutTotal;
    private Double beneficeNet;
    private Double scoreFinal;
    private String recommendation;
}
