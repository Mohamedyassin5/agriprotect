package tn.esprit.agri.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import tn.esprit.agri.entities.AnalyseRentabilite;
import tn.esprit.agri.entities.Credit;
import tn.esprit.agri.entities.DemandeCredit;
import tn.esprit.agri.entities.enums.DecisionCredit;
import tn.esprit.agri.entities.enums.StatutCredit;
import tn.esprit.agri.entities.enums.StatutDemande;
import tn.esprit.agri.exception.BusinessRuleException;
import tn.esprit.agri.exception.NotFoundException;
import tn.esprit.agri.repositories.AnalyseRentabiliteRepository;
import tn.esprit.agri.repositories.CreditRepository;
import tn.esprit.agri.repositories.DemandeCreditRepository;
import tn.esprit.agri.services.AuditService;
import tn.esprit.agri.services.FinancialCalculationService;
import tn.esprit.agri.services.IDemandeCreditService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DemandeCreditServiceImpl implements IDemandeCreditService {

    private final DemandeCreditRepository demandeCreditRepository;
    private final AnalyseRentabiliteRepository analyseRentabiliteRepository;
    private final CreditRepository creditRepository;
    private final AuditService auditService;
    private final FinancialCalculationService financialCalculationService;

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
        auditService.log("DEMANDE", "CREATED", saved.getId(), null, saved, saved.getAgriculteurId());

        return mapToResponseDto(saved);
    }

    @Override
    public DemandeCreditResponseDto getDemandeById(Long id) {
        DemandeCredit demande = findDemandeOrThrow(id);
        return mapToResponseDto(demande);
    }

    @Override
    public List<DemandeCreditResponseDto> getDemandesByAgriculteur(String agriculteurId) {
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
            throw new BusinessRuleException("La demande ne peut plus être modifiée (statut: " + demande.getStatut() + ")");
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
        auditService.log("DEMANDE", "UPDATED", saved.getId(), null, saved, saved.getUpdatedBy());
        return mapToResponseDto(saved);
    }

    @Override
    @Transactional
    public void deleteDemande(Long id) {
        DemandeCredit demande = findDemandeOrThrow(id);

        if (demande.getStatut() != StatutDemande.NOUVELLE &&
                demande.getStatut() != StatutDemande.REJETEE) {
            throw new BusinessRuleException("Seules les demandes NOUVELLE ou REJETEE peuvent être supprimées");
        }

        demandeCreditRepository.delete(demande);
    }

    @Override
    public List<DemandeCreditResponseDto> getAllDemandes() {
        return demandeCreditRepository.findAll()
                .stream().map(this::mapToResponseDto).toList();
    }

    @Override
    public List<DemandeCreditResponseDto> getDemandesFiltered(DemandeCreditFilterDto filterDto) {
        org.springframework.data.domain.Sort sort = "asc".equalsIgnoreCase(filterDto.getDirection())
                ? org.springframework.data.domain.Sort.by(filterDto.getSortBy()).ascending()
                : org.springframework.data.domain.Sort.by(filterDto.getSortBy()).descending();
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(filterDto.getPage(), filterDto.getSize(), sort);
        org.springframework.data.domain.Page<DemandeCredit> page = demandeCreditRepository.findAll(pageable);

        return page.getContent().stream()
                .filter(d -> filterDto.getStatut() == null || d.getStatut() == filterDto.getStatut())
                .filter(d -> filterDto.getDateFrom() == null || !d.getDateDemande().isBefore(filterDto.getDateFrom()))
                .filter(d -> filterDto.getDateTo() == null || !d.getDateDemande().isAfter(filterDto.getDateTo()))
                .map(this::mapToResponseDto)
                .toList();
    }

    private DemandeCredit findDemandeOrThrow(Long id) {
        return demandeCreditRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Demande de crédit non trouvée : " + id));
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
            throw new BusinessRuleException("Une analyse existe déjà pour cette demande");
        }

        if (demande.getStatut() == StatutDemande.ACCEPTEE || demande.getStatut() == StatutDemande.REJETEE) {
            throw new BusinessRuleException("Impossible d'ajouter une analyse sur une demande déjà finalisée");
        }

        double benefice = dto.getRevenuBrut() - dto.getCoutTotal();

        CreditScoringDto scoring = scoreDemande(demandeId);
        AnalyseRentabilite analyse = AnalyseRentabilite.builder()
                .revenuBrut(dto.getRevenuBrut())
                .coutTotal(dto.getCoutTotal())
                .beneficeNet(financialCalculationService.roundMoney(benefice))
                .decision(dto.getDecision())
                .commentaire(dto.getCommentaire() != null ? dto.getCommentaire().trim() : null)
                .dateAnalyse(LocalDateTime.now())
                .demandeCredit(demande)
                .analysteId(getCurrentUserId())
                .scoreRevenueStability(scoring.getRevenueStabilityScore())
                .scoreDebtRatio(scoring.getDebtRatioScore())
                .scoreProjectRisk(scoring.getProjectRiskScore())
                .scoreHistory(scoring.getHistoryScore())
                .scoreFinal(scoring.getFinalScore())
                .recommendation(scoring.getRecommendation())
                .build();

        demande.setAnalyseRentabilite(analyse);

        if (dto.getDecision() == DecisionCredit.ACCEPTEE) {
            demande.setStatut(StatutDemande.ACCEPTEE);
        } else if (dto.getDecision() == DecisionCredit.REFUSEE) {
            demande.setStatut(StatutDemande.REJETEE);
        } else {
            demande.setStatut(StatutDemande.EN_COURS_INSTRUCTION);
        }

        demandeCreditRepository.save(demande);
        auditService.log("ANALYSE", "CREATED", analyse.getId(), null, analyse, analyse.getAnalysteId());

        return mapToAnalyseResponseDto(analyse);
    }

    @Override
    public AnalyseRentabiliteResponseDto getAnalyseByDemandeId(Long demandeId) {
        DemandeCredit demande = findDemandeOrThrow(demandeId);
        if (demande.getAnalyseRentabilite() == null) {
            throw new NotFoundException("Aucune analyse de rentabilité trouvée pour la demande " + demandeId);
        }
        return mapToAnalyseResponseDto(demande.getAnalyseRentabilite());
    }

    @Override
    @Transactional
    public AnalyseRentabiliteResponseDto updateAnalyseRentabilite(Long analyseId, AnalyseRentabiliteCreateDto dto) {
        AnalyseRentabilite analyse = analyseRentabiliteRepository.findById(analyseId)
                .orElseThrow(() -> new NotFoundException("Analyse de rentabilité non trouvée : " + analyseId));

        analyse.setRevenuBrut(dto.getRevenuBrut());
        analyse.setCoutTotal(dto.getCoutTotal());
        analyse.setBeneficeNet(dto.getRevenuBrut() - dto.getCoutTotal());
        analyse.setDecision(dto.getDecision());
        analyse.setCommentaire(dto.getCommentaire() != null ? dto.getCommentaire().trim() : null);
        analyse.setDateAnalyse(LocalDateTime.now());

        DemandeCredit demande = analyse.getDemandeCredit();
        if (dto.getDecision() == DecisionCredit.ACCEPTEE) {
            demande.setStatut(StatutDemande.ACCEPTEE);
        } else if (dto.getDecision() == DecisionCredit.REFUSEE) {
            demande.setStatut(StatutDemande.REJETEE);
        } else {
            demande.setStatut(StatutDemande.EN_COURS_INSTRUCTION);
        }

        analyseRentabiliteRepository.save(analyse);
        demandeCreditRepository.save(demande);
        auditService.log("ANALYSE", "UPDATED", analyseId, null, analyse, analyse.getAnalysteId());
        return mapToAnalyseResponseDto(analyse);
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
                .scoreFinal(a.getScoreFinal())
                .recommendation(a.getRecommendation())
                .build();
    }

    @Override
    @Transactional
    public CreditResponseDto creerCreditDepuisDemande(Long demandeId, CreationCreditDto dto) {
        DemandeCredit demande = findDemandeOrThrow(demandeId);

        if (demande.getStatut() != StatutDemande.ACCEPTEE) {
            throw new BusinessRuleException("Seules les demandes ACCEPTÉES peuvent être transformées en crédit");
        }

        if (demande.getCredit() != null) {
            throw new BusinessRuleException("Un crédit existe déjà pour cette demande");
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


        creditRepository.save(credit);
        auditService.log("CREDIT", "CREATED", credit.getId(), null, credit, credit.getAgriculteurId());

        return mapToCreditResponseDto(credit);
    }

    @Override
    public CreditResponseDto getCreditByDemandeId(Long demandeId) {
        Credit credit = creditRepository.findByDemandeCreditId(demandeId)
                .orElseThrow(() -> new NotFoundException("Aucun crédit trouvé pour la demande : " + demandeId));
        return mapToCreditResponseDto(credit);
    }

    @Override
    public CreditResponseDto getCreditById(Long creditId) {
        Credit credit = creditRepository.findById(creditId)
                .orElseThrow(() -> new NotFoundException("Crédit non trouvé : " + creditId));
        return mapToCreditResponseDto(credit);
    }

    @Override
    @Transactional
    public DemandeCreditResponseDto startInstruction(Long demandeId, String actorId) {
        DemandeCredit demande = findDemandeOrThrow(demandeId);
        validateTransition(demande.getStatut(), StatutDemande.EN_COURS_INSTRUCTION);
        demande.setStatut(StatutDemande.EN_COURS_INSTRUCTION);
        demande.setInstructionAt(LocalDateTime.now());
        demande.setUpdatedBy(actorId != null ? actorId : getCurrentUserId());
        demandeCreditRepository.save(demande);
        auditService.log("DEMANDE", "STARTED_INSTRUCTION", demande.getId(), null, demande, demande.getUpdatedBy());
        return mapToResponseDto(demande);
    }

    @Override
    @Transactional
    public DemandeCreditResponseDto finaliserDecision(Long demandeId, DecisionFinaleDto dto) {
        DemandeCredit demande = findDemandeOrThrow(demandeId);
        StatutDemande target = dto.getDecision() == DecisionCredit.ACCEPTEE ? StatutDemande.ACCEPTEE : StatutDemande.REJETEE;
        validateTransition(demande.getStatut(), target);
        demande.setStatut(target);
        demande.setDecisionAt(LocalDateTime.now());
        demande.setUpdatedBy(dto.getActorId() != null ? dto.getActorId() : getCurrentUserId());
        demandeCreditRepository.save(demande);
        auditService.log("DEMANDE", "DECISION_FINALIZED", demande.getId(), null, demande, demande.getUpdatedBy());
        return mapToResponseDto(demande);
    }

    @Override
    @Transactional
    public DemandeCreditResponseDto archiveDemande(Long demandeId, String actorId) {
        DemandeCredit demande = findDemandeOrThrow(demandeId);
        validateTransition(demande.getStatut(), StatutDemande.ARCHIVEE);
        demande.setStatut(StatutDemande.ARCHIVEE);
        demande.setUpdatedBy(actorId != null ? actorId : getCurrentUserId());
        demandeCreditRepository.save(demande);
        auditService.log("DEMANDE", "ARCHIVED", demande.getId(), null, demande, demande.getUpdatedBy());
        return mapToResponseDto(demande);
    }

    @Override
    @Transactional
    public DemandeCreditResponseDto cancelDemande(Long demandeId, String actorId) {
        DemandeCredit demande = findDemandeOrThrow(demandeId);
        validateTransition(demande.getStatut(), StatutDemande.ANNULEE);
        demande.setStatut(StatutDemande.ANNULEE);
        demande.setUpdatedBy(actorId != null ? actorId : getCurrentUserId());
        demandeCreditRepository.save(demande);
        auditService.log("DEMANDE", "CANCELLED", demande.getId(), null, demande, demande.getUpdatedBy());
        return mapToResponseDto(demande);
    }

    @Override
    public CreditScoringDto scoreDemande(Long demandeId) {
        DemandeCredit demande = findDemandeOrThrow(demandeId);
        AnalyseRentabilite analyse = demande.getAnalyseRentabilite();

        double revenu = analyse != null ? analyse.getRevenuBrut() : 0.0;
        double cout = analyse != null ? analyse.getCoutTotal() : 0.0;
        double ratioDette = revenu == 0 ? 1.0 : Math.min(1.0, cout / revenu);

        double revenueStability = clamp(40 + (revenu / 1000.0), 0, 100);
        double debtRatioScore = clamp((1 - ratioDette) * 100, 0, 100);
        double projectRisk = clamp(100 - (demande.getMontantDemande() / 500.0), 0, 100);
        double history = 60.0;

        double finalScore = financialCalculationService.roundMoney(
                (revenueStability * 0.3) + (debtRatioScore * 0.3) + (projectRisk * 0.25) + (history * 0.15));
        String recommendation = finalScore >= 75 ? "AUTO_ACCEPT"
                : finalScore >= 55 ? "MANUAL_REVIEW"
                : "AUTO_REJECT";

        return CreditScoringDto.builder()
                .revenueStabilityScore(financialCalculationService.roundMoney(revenueStability))
                .debtRatioScore(financialCalculationService.roundMoney(debtRatioScore))
                .projectRiskScore(financialCalculationService.roundMoney(projectRisk))
                .historyScore(history)
                .finalScore(finalScore)
                .recommendation(recommendation)
                .build();
    }

    @Override
    public DemandeAnalysisReportDto buildDemandeReport(Long demandeId) {
        DemandeCredit demande = findDemandeOrThrow(demandeId);
        AnalyseRentabilite analyse = demande.getAnalyseRentabilite();
        CreditScoringDto score = scoreDemande(demandeId);
        return DemandeAnalysisReportDto.builder()
                .demandeId(demande.getId())
                .agriculteurId(demande.getAgriculteurId())
                .statut(demande.getStatut())
                .montantDemande(demande.getMontantDemande())
                .revenuBrut(analyse != null ? analyse.getRevenuBrut() : null)
                .coutTotal(analyse != null ? analyse.getCoutTotal() : null)
                .beneficeNet(analyse != null ? analyse.getBeneficeNet() : null)
                .scoreFinal(score.getFinalScore())
                .recommendation(score.getRecommendation())
                .build();
    }

    private String getCurrentUserId() {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }
        Object principal = auth.getPrincipal();

        if (principal instanceof String s) {
            return s;
        }
        if (principal instanceof tn.esprit.agri.entities.User u) {
            return u.getId();
        }
        return null;
    }

    private void validateTransition(StatutDemande from, StatutDemande to) {
        boolean valid = switch (from) {
            case NOUVELLE -> to == StatutDemande.EN_COURS_INSTRUCTION || to == StatutDemande.ANNULEE;
            case EN_COURS_INSTRUCTION -> to == StatutDemande.ACCEPTEE || to == StatutDemande.REJETEE || to == StatutDemande.ANNULEE;
            case ACCEPTEE, REJETEE, ANNULEE -> to == StatutDemande.ARCHIVEE;
            case ARCHIVEE, ANALYSE_TERMINEE -> false;
        };
        if (!valid) {
            throw new BusinessRuleException("Transition invalide: " + from + " -> " + to);
        }
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
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
