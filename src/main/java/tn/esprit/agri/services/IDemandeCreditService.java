package tn.esprit.agri.services;

import tn.esprit.agri.DTO.AnalyseDTO.AnalyseRentabiliteCreateDto;
import tn.esprit.agri.DTO.AnalyseDTO.AnalyseRentabiliteResponseDto;
import tn.esprit.agri.DTO.AnalyseDTO.CreditScoringDto;
import tn.esprit.agri.DTO.AnalyseDTO.DemandeAnalysisReportDto;
import tn.esprit.agri.DTO.CreditDTO.CreationCreditDto;
import tn.esprit.agri.DTO.CreditDTO.CreditResponseDto;
import tn.esprit.agri.DTO.DemandeDTO.CreationDemandeCreditDto;
import tn.esprit.agri.DTO.DemandeDTO.DecisionFinaleDto;
import tn.esprit.agri.DTO.DemandeDTO.DemandeCreditFilterDto;
import tn.esprit.agri.DTO.DemandeDTO.DemandeCreditResponseDto;
import tn.esprit.agri.DTO.DemandeDTO.UpdateDemandeCreditDto;

import java.util.List;

public interface IDemandeCreditService {

    DemandeCreditResponseDto creerDemande(CreationDemandeCreditDto dto);

    DemandeCreditResponseDto getDemandeById(Long id);

    List<DemandeCreditResponseDto> getDemandesByAgriculteur(String agriculteurId);

    List<DemandeCreditResponseDto> getDemandesEnCours();

    DemandeCreditResponseDto updateDemande(Long id, UpdateDemandeCreditDto dto);

    void deleteDemande(Long id);

    List<DemandeCreditResponseDto> getAllDemandes();
    List<DemandeCreditResponseDto> getDemandesFiltered(DemandeCreditFilterDto filterDto);

    AnalyseRentabiliteResponseDto creerAnalyseRentabilite(Long demandeId, AnalyseRentabiliteCreateDto dto);

    AnalyseRentabiliteResponseDto getAnalyseByDemandeId(Long demandeId);

    AnalyseRentabiliteResponseDto updateAnalyseRentabilite(Long analyseId, AnalyseRentabiliteCreateDto dto);

    CreditResponseDto creerCreditDepuisDemande(Long demandeId, CreationCreditDto dto);

    CreditResponseDto getCreditByDemandeId(Long demandeId);

    CreditResponseDto getCreditById(Long creditId);

    DemandeCreditResponseDto startInstruction(Long demandeId, String actorId);

    DemandeCreditResponseDto finaliserDecision(Long demandeId, DecisionFinaleDto dto);

    DemandeCreditResponseDto archiveDemande(Long demandeId, String actorId);

    DemandeCreditResponseDto cancelDemande(Long demandeId, String actorId);

    CreditScoringDto scoreDemande(Long demandeId);

    DemandeAnalysisReportDto buildDemandeReport(Long demandeId);
}
