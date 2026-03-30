package tn.esprit.agri.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.agri.DTO.CreditDTO.CreditSimulationRequestDto;
import tn.esprit.agri.DTO.CreditDTO.CreditSimulationResponseDto;
import tn.esprit.agri.DTO.CreditDTO.CreditSimulationResultDto;
import tn.esprit.agri.DTO.CreditDTO.PortfolioAlertDto;
import tn.esprit.agri.DTO.CreditDTO.PortfolioKpiDto;
import tn.esprit.agri.entities.Credit;
import tn.esprit.agri.entities.Echeance;
import tn.esprit.agri.entities.enums.StatutEcheance;
import tn.esprit.agri.repositories.CreditRepository;
import tn.esprit.agri.services.FinancialCalculationService;
import tn.esprit.agri.services.PortfolioAnalyticsService;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PortfolioAnalyticsServiceImpl implements PortfolioAnalyticsService {
    private final FinancialCalculationService financialCalculationService;
    private final CreditRepository creditRepository;

    @Override
    public CreditSimulationResponseDto simulateOffers(CreditSimulationRequestDto requestDto) {
        List<CreditSimulationResultDto> results = requestDto.getOffres().stream()
                .map(o -> financialCalculationService.simulate(o.getMontant(), o.getTauxInteret(), o.getDureeMois(), o.getFrais()))
                .sorted(resolveComparator(requestDto.getCriteria()))
                .toList();

        return CreditSimulationResponseDto.builder()
                .criteria(requestDto.getCriteria())
                .rankedOffers(results)
                .build();
    }

    @Override
    public PortfolioKpiDto computeKpis() {
        List<Credit> credits = creditRepository.findAll();
        double totalOutstanding = credits.stream().mapToDouble(c -> outstandingForCredit(c)).sum();

        long totalEcheances = credits.stream().mapToLong(c -> c.getEcheances().size()).sum();
        long overdueCount = credits.stream().flatMap(c -> c.getEcheances().stream())
                .filter(e -> e.getStatut() == StatutEcheance.ECHUE_NON_PAYEE).count();
        long impayeCount = credits.stream().flatMap(c -> c.getEcheances().stream())
                .filter(e -> e.getStatut() == StatutEcheance.IMPAYEE_DEFINITIVE).count();
        long paidCount = credits.stream().flatMap(c -> c.getEcheances().stream())
                .filter(e -> e.getStatut() == StatutEcheance.PAYEE_A_TEMPS || e.getStatut() == StatutEcheance.PAYEE_EN_RETARD).count();

        double par30 = totalEcheances == 0 ? 0.0 : ((double) overdueCount / totalEcheances) * 100.0;
        double defaultRatio = totalEcheances == 0 ? 0.0 : ((double) impayeCount / totalEcheances) * 100.0;
        double collectionRate = totalEcheances == 0 ? 0.0 : ((double) paidCount / totalEcheances) * 100.0;

        return PortfolioKpiDto.builder()
                .totalOutstanding(financialCalculationService.roundMoney(totalOutstanding))
                .par30Ratio(financialCalculationService.roundMoney(par30))
                .defaultRatioProxy(financialCalculationService.roundMoney(defaultRatio))
                .collectionRate(financialCalculationService.roundMoney(collectionRate))
                .build();
    }

    @Override
    public List<PortfolioAlertDto> computeAlerts() {
        PortfolioKpiDto kpi = computeKpis();
        List<Credit> credits = creditRepository.findAll();
        long repeatedPartials = credits.stream().flatMap(c -> c.getEcheances().stream())
                .filter(e -> e.getStatut() == StatutEcheance.PAYEE_PARTIELLEMENT).count();

        java.util.ArrayList<PortfolioAlertDto> alerts = new java.util.ArrayList<>();
        if (kpi.getPar30Ratio() > 20.0) {
            alerts.add(PortfolioAlertDto.builder().code("PAR30_HIGH").message("PAR30 dépasse 20%").build());
        }
        if (repeatedPartials >= 3) {
            alerts.add(PortfolioAlertDto.builder().code("REPEATED_PARTIAL_PAYMENT").message("Paiements partiels répétés détectés").build());
        }
        boolean overdueToday = credits.stream().flatMap(c -> c.getEcheances().stream())
                .anyMatch(e -> e.getDateEcheance().isBefore(LocalDate.now()) &&
                        (e.getStatut() == StatutEcheance.A_VENIR || e.getStatut() == StatutEcheance.PAYEE_PARTIELLEMENT));
        if (overdueToday) {
            alerts.add(PortfolioAlertDto.builder().code("OVERDUE_INSTALLMENTS").message("Échéances en retard détectées").build());
        }
        return alerts;
    }

    private Comparator<CreditSimulationResultDto> resolveComparator(String criteria) {
        if ("MIN_MENSUALITE".equalsIgnoreCase(criteria)) {
            return Comparator.comparing(CreditSimulationResultDto::getMensualite);
        }
        return Comparator.comparing(CreditSimulationResultDto::getTotalCost);
    }

    private double outstandingForCredit(Credit credit) {
        double paid = credit.getEcheances().stream().mapToDouble(e -> value(e.getMontantPaye())).sum();
        return Math.max(0.0, value(credit.getMontant()) - paid);
    }

    private double value(Double n) {
        return n == null ? 0.0 : n;
    }
}
