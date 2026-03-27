package tn.esprit.agri.services;

import tn.esprit.agri.DTO.DemandeDTO.CreationDemandeCreditDto;
import tn.esprit.agri.DTO.DemandeDTO.DemandeCreditResponseDto;
import tn.esprit.agri.DTO.DemandeDTO.UpdateDemandeCreditDto;

import java.util.List;

public interface IDemandeCreditService {

    DemandeCreditResponseDto creerDemande(CreationDemandeCreditDto dto);

    DemandeCreditResponseDto getDemandeById(Long id);

    List<DemandeCreditResponseDto> getDemandesByAgriculteur(Long agriculteurId);

    List<DemandeCreditResponseDto> getDemandesEnCours();

    DemandeCreditResponseDto updateDemande(Long id, UpdateDemandeCreditDto dto);

    void deleteDemande(Long id);

    List<DemandeCreditResponseDto> getAllDemandes();

    AnalyseRentabiliteResponseDto creerAnalyseRentabilite(Long demandeId, AnalyseRentabiliteCreateDto dto);

    AnalyseRentabiliteResponseDto getAnalyseByDemandeId(Long demandeId);

    AnalyseRentabiliteResponseDto updateAnalyseRentabilite(Long analyseId, AnalyseRentabiliteCreateDto dto);

    CreditResponseDto creerCreditDepuisDemande(Long demandeId, CreationCreditDto dto);

    CreditResponseDto getCreditByDemandeId(Long demandeId);

    CreditResponseDto getCreditById(Long creditId);
}
