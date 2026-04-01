package tn.esprit.agri.services;

import tn.esprit.agri.DTO.CreditDTO.CreditSimulationResultDto;

public interface FinancialCalculationService {
    double roundMoney(double value);
    double calculateMensualite(double principal, double annualRate, int months);
    double calculateTotalInterets(double mensualite, double principal, int months);
    double calculateOutstandingPrincipal(double principal, double annualRate, int months, int paidMonths);
    double calculatePenalty(double amountDue, int delayDays);
    CreditSimulationResultDto simulate(double montant, double tauxInteret, int dureeMois, double frais);
}
