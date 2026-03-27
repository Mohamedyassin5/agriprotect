package tn.esprit.agri.DTO.EcheanceDTO;

import lombok.Builder;
import lombok.Getter;
import tn.esprit.agri.entities.enums.StatutEcheance;

import java.time.LocalDate;

@Getter
@Builder
public class EcheanceResponseDto {
    private Long id;
    private LocalDate dateEcheance;
    private Double montantDu;
    private Double montantPaye;
    private StatutEcheance statut;
    private Double capitalDu;
    private Double interetsDu;
    private Double assuranceDu;
    private LocalDate datePaiementEffectif;
    private Integer numeroEcheance;
    private Long creditId;
}