package tn.esprit.agri.DTO.CreditDTO;

import lombok.*;
import tn.esprit.agri.entities.enums.StatutCredit;

import java.time.LocalDate;

@Getter
@Builder
public class CreditResponseDto {
    private Long id;
    private Double montant;
    private Double tauxInteret;
    private Integer dureeMois;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private StatutCredit statut;
    private String agriculteurId;
    private Long demandeCreditId;
    private Long assuranceId;
    private String referenceContrat;
}