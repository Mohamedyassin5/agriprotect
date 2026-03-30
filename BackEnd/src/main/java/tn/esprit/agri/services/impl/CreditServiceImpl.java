package tn.esprit.agri.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.agri.DTO.EcheanceDTO.EcheancePaiementDto;
import tn.esprit.agri.DTO.EcheanceDTO.EcheanceResponseDto;
import tn.esprit.agri.entities.Credit;
import tn.esprit.agri.entities.Echeance;
import tn.esprit.agri.entities.enums.StatutCredit;
import tn.esprit.agri.entities.enums.StatutEcheance;
import tn.esprit.agri.exceptions.BusinessRuleException;
import tn.esprit.agri.exceptions.NotFoundException;
import tn.esprit.agri.repositories.CreditRepository;
import tn.esprit.agri.repositories.EcheanceRepository;
import tn.esprit.agri.services.AuditService;
import tn.esprit.agri.services.FinancialCalculationService;
import tn.esprit.agri.services.ICreditService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CreditServiceImpl implements ICreditService {

    private final CreditRepository creditRepository;
    private final EcheanceRepository echeanceRepository;
    private final FinancialCalculationService financialCalculationService;
    private final AuditService auditService;

    @Override
    @Transactional
    public List<EcheanceResponseDto> genererEcheancier(Long creditId) {
        Credit credit = creditRepository.findById(creditId)
                .orElseThrow(() -> new NotFoundException("Crédit non trouvé"));

        if (!credit.getEcheances().isEmpty()) {
            throw new BusinessRuleException("L'échéancier existe déjà pour ce crédit");
        }

        List<Echeance> echeances = new ArrayList<>();
        LocalDate dateCourante = credit.getDateDebut();
        double capitalRestant = credit.getMontant();
        double mensualite = financialCalculationService.calculateMensualite(
                credit.getMontant(),
                credit.getTauxInteret(),
                credit.getDureeMois());
        double tauxMensuel = credit.getTauxInteret() / 100 / 12;

        for (int i = 1; i <= credit.getDureeMois(); i++) {
            double interets = financialCalculationService.roundMoney(capitalRestant * tauxMensuel);
            double amortissementCapital = financialCalculationService.roundMoney(mensualite - interets);
            double capitalApres = financialCalculationService.roundMoney(capitalRestant - amortissementCapital);

            Echeance echeance = Echeance.builder()
                    .numeroEcheance(i)
                    .dateEcheance(dateCourante)
                    .montantDu(financialCalculationService.roundMoney(mensualite))
                    .montantPaye(0.0)
                    .capitalDu(amortissementCapital)
                    .interetsDu(interets)
                    .assuranceDu(0.0)
                    .joursRetard(0)
                    .penalite(0.0)
                    .statut(StatutEcheance.A_VENIR)
                    .credit(credit)
                    .build();

            echeances.add(echeance);

            capitalRestant = capitalApres;
            dateCourante = dateCourante.plusMonths(1);
        }

        credit.getEcheances().addAll(echeances);
        creditRepository.save(credit);
        auditService.log("CREDIT", "SCHEDULE_GENERATED", creditId, null, echeances, null);

        return echeances.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EcheanceResponseDto enregistrerPaiement(Long echeanceId, EcheancePaiementDto dto) {
        Echeance echeance = echeanceRepository.findById(echeanceId)
                .orElseThrow(() -> new NotFoundException("Échéance non trouvée"));
        Echeance before = Echeance.builder()
                .id(echeance.getId())
                .montantPaye(echeance.getMontantPaye())
                .statut(echeance.getStatut())
                .datePaiementEffectif(echeance.getDatePaiementEffectif())
                .build();

        if (echeance.getStatut() == StatutEcheance.PAYEE_A_TEMPS || 
            echeance.getStatut() == StatutEcheance.PAYEE_EN_RETARD) {
            throw new BusinessRuleException("Échéance déjà payée");
        }

        double nouveauPaye = financialCalculationService.roundMoney(echeance.getMontantPaye() + dto.getMontantPaye());
        echeance.setMontantPaye(nouveauPaye);
        LocalDate datePaiement = dto.getDatePaiement() != null ? dto.getDatePaiement() : LocalDate.now();
        echeance.setDatePaiementEffectif(datePaiement);
        echeance.setReferencePaiement(dto.getReferencePaiement());

        if (nouveauPaye >= echeance.getMontantDu()) {
            if (datePaiement.isAfter(echeance.getDateEcheance())) {
                echeance.setStatut(StatutEcheance.PAYEE_EN_RETARD);
                int retard = (int) java.time.temporal.ChronoUnit.DAYS.between(echeance.getDateEcheance(), datePaiement);
                echeance.setJoursRetard(retard);
                echeance.setPenalite(financialCalculationService.calculatePenalty(echeance.getMontantDu(), retard));
            } else {
                echeance.setStatut(StatutEcheance.PAYEE_A_TEMPS);
                echeance.setJoursRetard(0);
                echeance.setPenalite(0.0);
            }
        } else if (nouveauPaye > 0) {
            echeance.setStatut(StatutEcheance.PAYEE_PARTIELLEMENT);
        }

        updateCreditAfterPayment(echeance.getCredit());

        echeanceRepository.save(echeance);
        auditService.log("ECHEANCE", "PAYMENT_REGISTERED", echeance.getId(), before, echeance, null);
        return mapToResponseDto(echeance);
    }

    @Override
    public List<EcheanceResponseDto> getEcheancesByCredit(Long creditId) {
        if (!creditRepository.existsById(creditId)) {
            throw new NotFoundException("Crédit non trouvé");
        }
        return echeanceRepository.findByCreditIdOrderByNumeroEcheanceAsc(creditId)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public EcheanceResponseDto getEcheanceById(Long id) {
        Echeance echeance = echeanceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Échéance non trouvée"));
        return mapToResponseDto(echeance);
    }

    @Override
    public List<EcheanceResponseDto> getUpcomingEcheances(Long creditId) {
        return echeanceRepository.findByCreditIdAndStatutInOrderByNumeroEcheanceAsc(
                        creditId, List.of(StatutEcheance.A_VENIR))
                .stream().map(this::mapToResponseDto).toList();
    }

    @Override
    public List<EcheanceResponseDto> getOverdueEcheances(Long creditId) {
        return echeanceRepository.findByCreditIdAndStatutInOrderByNumeroEcheanceAsc(
                        creditId, List.of(StatutEcheance.ECHUE_NON_PAYEE, StatutEcheance.IMPAYEE_DEFINITIVE))
                .stream().map(this::mapToResponseDto).toList();
    }

    @Override
    public List<EcheanceResponseDto> getPaidEcheances(Long creditId) {
        return echeanceRepository.findByCreditIdAndStatutInOrderByNumeroEcheanceAsc(
                        creditId, List.of(StatutEcheance.PAYEE_A_TEMPS, StatutEcheance.PAYEE_EN_RETARD, StatutEcheance.PAYEE_PARTIELLEMENT))
                .stream().map(this::mapToResponseDto).toList();
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0 2 * * *")
    public int markOverdueEcheances() {
        List<Echeance> overdueCandidates = echeanceRepository.findByDateEcheanceBeforeAndStatutIn(
                LocalDate.now(), List.of(StatutEcheance.A_VENIR, StatutEcheance.PAYEE_PARTIELLEMENT));

        for (Echeance e : overdueCandidates) {
            int delayDays = (int) java.time.temporal.ChronoUnit.DAYS.between(e.getDateEcheance(), LocalDate.now());
            e.setJoursRetard(delayDays);
            e.setPenalite(financialCalculationService.calculatePenalty(e.getMontantDu(), delayDays));
            e.setStatut(StatutEcheance.ECHUE_NON_PAYEE);
        }
        echeanceRepository.saveAll(overdueCandidates);
        return overdueCandidates.size();
    }

    private void updateCreditAfterPayment(Credit credit) {
        double montantRembourse = credit.getEcheances().stream()
                .mapToDouble(e -> e.getMontantPaye() == null ? 0.0 : e.getMontantPaye())
                .sum();

        LocalDate derniereDate = credit.getEcheances().stream()
                .filter(e -> e.getStatut() == StatutEcheance.PAYEE_A_TEMPS || e.getStatut() == StatutEcheance.PAYEE_EN_RETARD)
                .map(Echeance::getDatePaiementEffectif)
                .filter(d -> d != null)
                .max(LocalDate::compareTo)
                .orElse(credit.getDerniereEcheancePayee());

        credit.setMontantRembourse(montantRembourse);
        credit.setDerniereEcheancePayee(derniereDate);

        boolean toutesPayees = credit.getEcheances().stream()
                .allMatch(e -> e.getStatut() == StatutEcheance.PAYEE_A_TEMPS || e.getStatut() == StatutEcheance.PAYEE_EN_RETARD);
        if (toutesPayees && !credit.getEcheances().isEmpty()) {
            credit.setStatut(StatutCredit.REMBOURSE);
        } else if (montantRembourse > 0) {
            credit.setStatut(StatutCredit.EN_COURS);
        }

        creditRepository.save(credit);
    }

    private EcheanceResponseDto mapToResponseDto(Echeance e) {
        return EcheanceResponseDto.builder()
                .id(e.getId())
                .dateEcheance(e.getDateEcheance())
                .montantDu(e.getMontantDu())
                .montantPaye(e.getMontantPaye())
                .statut(e.getStatut())
                .capitalDu(e.getCapitalDu())
                .interetsDu(e.getInteretsDu())
                .assuranceDu(e.getAssuranceDu())
                .datePaiementEffectif(e.getDatePaiementEffectif())
                .referencePaiement(e.getReferencePaiement())
                .joursRetard(e.getJoursRetard())
                .penalite(e.getPenalite())
                .numeroEcheance(e.getNumeroEcheance())
                .creditId(e.getCredit().getId())
                .build();
    }

}