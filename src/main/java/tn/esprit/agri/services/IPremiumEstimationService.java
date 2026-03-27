package tn.esprit.agri.services;

import tn.esprit.agri.dto.PremiumEstimationResponse;
import tn.esprit.agri.entities.enums.CoverageType;

import java.math.BigDecimal;

public interface IPremiumEstimationService {
    PremiumEstimationResponse calculateEstimation(String userId, CoverageType selectedCoverType);
}