package tn.esprit.agri.services;

import tn.esprit.agri.entities.Crop;

import java.util.List;

public interface ICreditService {
    List<EcheanceResponseDto> genererEcheancier(Long creditId);

    EcheanceResponseDto enregistrerPaiement(Long echeanceId, EcheancePaiementDto dto);

    List<EcheanceResponseDto> getEcheancesByCredit(Long creditId);

    EcheanceResponseDto getEcheanceById(Long id);
}