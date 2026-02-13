package tn.esprit.agri.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.agri.DTO.DemandeDTO.CreationDemandeCreditDto;
import tn.esprit.agri.DTO.DemandeDTO.DemandeCreditResponseDto;
import tn.esprit.agri.DTO.DemandeDTO.UpdateDemandeCreditDto;
import tn.esprit.agri.entities.DemandeCredit;
import tn.esprit.agri.entities.enums.StatutDemande;
import tn.esprit.agri.repositories.DemandeCreditRepository;
import tn.esprit.agri.services.IDemandeCreditService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DemandeCreditServiceImpl implements IDemandeCreditService {

    private final DemandeCreditRepository demandeCreditRepository;

    @Override
    @Transactional
    public DemandeCreditResponseDto creerDemande(CreationDemandeCreditDto dto) {
        DemandeCredit demande = DemandeCredit.builder()
                .dateDemande(dto.getDateDemande())
                .agriculteurId(dto.getAgriculteurId())
                .montantDemande(dto.getMontantDemande())
                .description(dto.getDescription())
                .statut(StatutDemande.NOUVELLE)
                .build();

        DemandeCredit saved = demandeCreditRepository.save(demande);

        return mapToResponseDto(saved);
    }

    @Override
    public DemandeCreditResponseDto getDemandeById(Long id) {
        DemandeCredit demande = findDemandeOrThrow(id);
        return mapToResponseDto(demande);
    }

    @Override
    public List<DemandeCreditResponseDto> getDemandesByAgriculteur(Long agriculteurId) {
        return demandeCreditRepository.findByAgriculteurId(agriculteurId)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<DemandeCreditResponseDto> getDemandesEnCours() {
        return demandeCreditRepository.findByStatut(StatutDemande.EN_COURS_INSTRUCTION)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DemandeCreditResponseDto updateDemande(Long id, UpdateDemandeCreditDto dto) {
        DemandeCredit demande = findDemandeOrThrow(id);

        if (demande.getStatut() != StatutDemande.NOUVELLE) {
            throw new IllegalStateException("La demande ne peut plus être modifiée (statut: " + demande.getStatut() + ")");
        }

        if (dto.getDateDemande() != null) {
            demande.setDateDemande(dto.getDateDemande());
        }
        if (dto.getMontantDemande() != null) {
            demande.setMontantDemande(dto.getMontantDemande());
        }
        if (dto.getDescription() != null) {
            demande.setDescription(dto.getDescription().trim());
        }

        DemandeCredit saved = demandeCreditRepository.save(demande);
        return mapToResponseDto(saved);
    }

    @Override
    @Transactional
    public void deleteDemande(Long id) {
        DemandeCredit demande = findDemandeOrThrow(id);

        if (demande.getStatut() != StatutDemande.NOUVELLE &&
                demande.getStatut() != StatutDemande.REJETEE) {
            throw new IllegalStateException("Seules les demandes NOUVELLE ou REJETEE peuvent être supprimées");
        }

        demandeCreditRepository.delete(demande);
    }

    @Override
    public List<DemandeCreditResponseDto> getAllDemandes() {
        return demandeCreditRepository.findAll()
                .stream().map(this::mapToResponseDto).toList();
    }

    private DemandeCredit findDemandeOrThrow(Long id) {
        return demandeCreditRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande de crédit non trouvée : " + id));
    }

    private DemandeCreditResponseDto mapToResponseDto(DemandeCredit entity) {
        return DemandeCreditResponseDto.builder()
                .id(entity.getId())
                .dateDemande(entity.getDateDemande())
                .statut(entity.getStatut())
                .agriculteurId(entity.getAgriculteurId())
                .montantDemande(entity.getMontantDemande())
                .description(entity.getDescription())
                .build();
    }
}
