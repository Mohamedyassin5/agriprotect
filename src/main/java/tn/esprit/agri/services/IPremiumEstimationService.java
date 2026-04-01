package tn.esprit.agri.services;

import tn.esprit.agri.DTO.PremiumEstimationResponse;
import tn.esprit.agri.entities.enums.CoverageType;

public interface IPremiumEstimationService {
    PremiumEstimationResponse calculateEstimation(String userId, CoverageType selectedCoverType);
}