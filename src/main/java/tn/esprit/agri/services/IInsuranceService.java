package tn.esprit.agri.services;

import tn.esprit.agri.dto.InsuranceResponse;
import tn.esprit.agri.dto.SignRequestDTO;
import tn.esprit.agri.entities.Insurance;
import tn.esprit.agri.entities.enums.CoverageType;
import tn.esprit.agri.entities.enums.Language;
import tn.esprit.agri.entities.enums.PaymentMode;

public interface IInsuranceService {

    InsuranceResponse subscribe(String userId, CoverageType coverType, PaymentMode paymentMode);

    byte[] generateInsuranceCertificatePdf(Insurance insurance, Language lang);

    /**
     * Signe la police et la passe à ACTIVE
     */
    Insurance signInsurance(String insuranceId, String userId, SignRequestDTO dto);
}