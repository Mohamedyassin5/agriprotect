package tn.esprit.agri.services;

import tn.esprit.agri.dto.InsuranceResponse;
import tn.esprit.agri.dto.SignRequestDTO;
import tn.esprit.agri.entities.Insurance;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.entities.enums.CoverageType;
import tn.esprit.agri.entities.enums.Language;
import tn.esprit.agri.entities.enums.PaymentMode;

import java.util.Map;

public interface IInsuranceService {

    InsuranceResponse subscribe(String userId, CoverageType coverType, PaymentMode paymentMode);

    byte[] generateInsuranceCertificatePdf(Insurance insurance, Language lang);

    /**
     * Signe la police et la passe à ACTIVE
     */
    Insurance signInsurance(String insuranceId, String userId, SignRequestDTO dto);
    Insurance findByIdWithAuthorization(String insuranceId, User currentUser);
    void cancelPendingSubscription(String insuranceId, String userId);
    /**
     * Retourne les données du tableau de bord pour un agriculteur
     */
    Map<String, Object> getFarmerDashboard(String userId);
}