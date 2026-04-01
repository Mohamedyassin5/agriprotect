package tn.esprit.agri.services.impl;

import org.springframework.stereotype.Service;
import tn.esprit.agri.DTO.CreditDTO.CreditSimulationResultDto;
import tn.esprit.agri.services.FinancialCalculationService;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class FinancialCalculationServiceImpl implements FinancialCalculationService {

    @Override
    public double roundMoney(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    @Override
    public double calculateMensualite(double principal, double annualRate, int months) {
        double monthlyRate = annualRate / 100.0 / 12.0;
        if (months <= 0) {
            return 0.0;
        }
        if (monthlyRate == 0) {
            return roundMoney(principal / months);
        }
        double result = principal * (monthlyRate * Math.pow(1 + monthlyRate, months))
                / (Math.pow(1 + monthlyRate, months) - 1);
        return roundMoney(result);
    }

    @Override
    public double calculateTotalInterets(double mensualite, double principal, int months) {
        return roundMoney((mensualite * months) - principal);
    }

    @Override
    public double calculateOutstandingPrincipal(double principal, double annualRate, int months, int paidMonths) {
        double monthlyRate = annualRate / 100.0 / 12.0;
        if (paidMonths <= 0) {
            return roundMoney(principal);
        }
        if (paidMonths >= months) {
            return 0.0;
        }
        if (monthlyRate == 0) {
            return roundMoney(principal - ((principal / months) * paidMonths));
        }
        double mensualite = calculateMensualite(principal, annualRate, months);
        double outstanding = principal * Math.pow(1 + monthlyRate, paidMonths)
                - mensualite * ((Math.pow(1 + monthlyRate, paidMonths) - 1) / monthlyRate);
        return roundMoney(Math.max(0.0, outstanding));
    }

    @Override
    public double calculatePenalty(double amountDue, int delayDays) {
        if (delayDays <= 0) {
            return 0.0;
        }
        return roundMoney(amountDue * 0.001 * delayDays);
    }

    @Override
    public CreditSimulationResultDto simulate(double montant, double tauxInteret, int dureeMois, double frais) {
        double mensualite = calculateMensualite(montant, tauxInteret, dureeMois);
        double totalInterets = calculateTotalInterets(mensualite, montant, dureeMois);
        double totalCost = roundMoney(montant + totalInterets + frais);
        double affordabilityRatio = roundMoney(mensualite / Math.max(1.0, montant) * 100.0);

        return CreditSimulationResultDto.builder()
                .montant(roundMoney(montant))
                .tauxInteret(roundMoney(tauxInteret))
                .dureeMois(dureeMois)
                .mensualite(mensualite)
                .totalInterets(totalInterets)
                .totalCost(totalCost)
                .affordabilityRatio(affordabilityRatio)
                .build();
    }
}
