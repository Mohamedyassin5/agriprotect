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


    @Override
    @Transactional
    public AnalyseRentabiliteResponseDto creerAnalyseRentabilite(Long demandeId, AnalyseRentabiliteCreateDto dto) {
        DemandeCredit demande = findDemandeOrThrow(demandeId);

        if (demande.getAnalyseRentabilite() != null) {
            throw new IllegalStateException("Une analyse existe déjà pour cette demande");
        }

        if (demande.getStatut() == StatutDemande.ACCEPTEE || demande.getStatut() == StatutDemande.REFUSEE) {
            throw new IllegalStateException("Impossible d'ajouter une analyse sur une demande déjà finalisée");
        }

        double benefice = dto.getRevenuBrut() - dto.getCoutTotal();

        AnalyseRentabilite analyse = AnalyseRentabilite.builder()
                .revenuBrut(dto.getRevenuBrut())
                .coutTotal(dto.getCoutTotal())
                .beneficeNet(benefice)
                .decision(dto.getDecision())
                .commentaire(dto.getCommentaire() != null ? dto.getCommentaire().trim() : null)
                .dateAnalyse(LocalDateTime.now())
                .demandeCredit(demande)
                .analysteId(getCurrentUserId())
                .build();

        demande.setAnalyseRentabilite(analyse);

        if (dto.getDecision() == DecisionCredit.ACCEPTEE) {
            demande.setStatut(StatutDemande.ACCEPTEE);
        } else if (dto.getDecision() == DecisionCredit.REFUSEE) {
            demande.setStatut(StatutDemande.REFUSEE);
        }

        demandeCreditRepository.save(demande);

        return mapToAnalyseResponseDto(analyse);
    }

    @Override
    public AnalyseRentabiliteResponseDto getAnalyseByDemandeId(Long demandeId) {
        DemandeCredit demande = findDemandeOrThrow(demandeId);
        if (demande.getAnalyseRentabilite() == null) {
            throw new RuntimeException("Aucune analyse de rentabilité trouvée pour la demande " + demandeId);
        }
        return mapToAnalyseResponseDto(demande.getAnalyseRentabilite());
    }

    private AnalyseRentabiliteResponseDto mapToAnalyseResponseDto(AnalyseRentabilite a) {
        return AnalyseRentabiliteResponseDto.builder()
                .id(a.getId())
                .revenuBrut(a.getRevenuBrut())
                .coutTotal(a.getCoutTotal())
                .beneficeNet(a.getBeneficeNet())
                .decision(a.getDecision())
                .commentaire(a.getCommentaire())
                .dateAnalyse(a.getDateAnalyse())
                .demandeCreditId(a.getDemandeCredit().getId())
                .analysteId(a.getAnalysteId())
                .build();
    }

    @Override
    @Transactional
    public CreditResponseDto creerCreditDepuisDemande(Long demandeId, CreationCreditDto dto) {
        DemandeCredit demande = findDemandeOrThrow(demandeId);

        if (demande.getStatut() != StatutDemande.ACCEPTEE) {
            throw new IllegalStateException("Seules les demandes ACCEPTÉES peuvent être transformées en crédit");
        }

        if (demande.getCredit() != null) {
            throw new IllegalStateException("Un crédit existe déjà pour cette demande");
        }

        LocalDate dateFinCalculee = dto.getDateDebut().plusMonths(dto.getDureeMois());

        Credit credit = Credit.builder()
                .montant(dto.getMontant())
                .tauxInteret(dto.getTauxInteret())
                .dureeMois(dto.getDureeMois())
                .dateDebut(dto.getDateDebut())
                .dateFin(dateFinCalculee)
                .statut(StatutCredit.DEBOURSE)
                .agriculteurId(demande.getAgriculteurId())
                .assuranceId(dto.getAssuranceId())
                .demandeCredit(demande)
                .referenceContrat("CR-" + System.currentTimeMillis())
                .build();

        demande.setCredit(credit);


        demandeCreditRepository.save(demande);

        return mapToCreditResponseDto(credit);
    }

    private CreditResponseDto mapToCreditResponseDto(Credit c) {
        return CreditResponseDto.builder()
                .id(c.getId())
                .montant(c.getMontant())
                .tauxInteret(c.getTauxInteret())
                .dureeMois(c.getDureeMois())
                .dateDebut(c.getDateDebut())
                .dateFin(c.getDateFin())
                .statut(c.getStatut())
                .agriculteurId(c.getAgriculteurId())
                .demandeCreditId(c.getDemandeCredit().getId())
                .assuranceId(c.getAssuranceId())
                .referenceContrat(c.getReferenceContrat())
                .build();
    }
}
