package tn.esprit.agri.services;

import tn.esprit.agri.DTO.CreditDTO.CreditSimulationRequestDto;
import tn.esprit.agri.DTO.CreditDTO.CreditSimulationResponseDto;
import tn.esprit.agri.DTO.CreditDTO.PortfolioAlertDto;
import tn.esprit.agri.DTO.CreditDTO.PortfolioKpiDto;

import java.util.List;

public interface PortfolioAnalyticsService {
    CreditSimulationResponseDto simulateOffers(CreditSimulationRequestDto requestDto);
    PortfolioKpiDto computeKpis();
    List<PortfolioAlertDto> computeAlerts();
}
