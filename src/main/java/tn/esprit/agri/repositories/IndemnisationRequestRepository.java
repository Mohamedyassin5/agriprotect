package tn.esprit.agri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.agri.entities.IndemnisationRequest;
import tn.esprit.agri.entities.IndemnisationRequest.RequestStatus;

import java.util.List;

public interface IndemnisationRequestRepository
                extends JpaRepository<IndemnisationRequest, String> {

        List<IndemnisationRequest> findByStatus(RequestStatus status);

        boolean existsByFarmerIdAndFundIdAndStatus(
                        String farmerId,
                        String fundId,
                        RequestStatus status);

        List<IndemnisationRequest> findByFarmerId(String farmerId);
}
