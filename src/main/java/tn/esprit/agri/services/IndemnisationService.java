package tn.esprit.agri.services;

import tn.esprit.agri.entities.IndemnisationRequest;
import java.util.List;

public interface IndemnisationService {

    IndemnisationRequest requestIndemnisation(
            String farmerId,
            String fundId,
            Double amount,
            String sinistreId,
            String farmerNotes,
            String damageType,
            Double affectedArea);

    java.util.List<tn.esprit.agri.entities.FarmerSolidarityFund> getMemberships(String farmerId);

    void processIndemnisation(String requestId, boolean approved, String refusalReason);

    java.util.List<IndemnisationRequest> getAllPendingRequests();

    java.util.List<IndemnisationRequest> getFarmerRequestHistory(String farmerId);
}
