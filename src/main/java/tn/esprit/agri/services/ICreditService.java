package tn.esprit.agri.services;

import tn.esprit.agri.DTO.EcheanceDTO.EcheancePaiementDto;
import tn.esprit.agri.DTO.EcheanceDTO.EcheanceResponseDto;

import java.util.List;

public interface ICreditService {
    List<EcheanceResponseDto> genererEcheancier(Long creditId);

    EcheanceResponseDto enregistrerPaiement(Long echeanceId, EcheancePaiementDto dto);

    List<EcheanceResponseDto> getEcheancesByCredit(Long creditId);

    EcheanceResponseDto getEcheanceById(Long id);

    List<EcheanceResponseDto> getUpcomingEcheances(Long creditId);

    List<EcheanceResponseDto> getOverdueEcheances(Long creditId);

    List<EcheanceResponseDto> getPaidEcheances(Long creditId);

    int markOverdueEcheances();
}