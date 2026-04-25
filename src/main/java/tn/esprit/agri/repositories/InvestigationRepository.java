package tn.esprit.agri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.agri.entities.Investigation;
import tn.esprit.agri.entities.enums.InvestigationStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvestigationRepository extends JpaRepository<Investigation, String> {
    List<Investigation> findByAssignedExpertIdOrderByCreatedAtDesc(String expertId);
    List<Investigation> findByFarmerIdOrderByCreatedAtDesc(String farmerId);
    boolean existsByIndemnisationRequestId(String indemnisationRequestId);
    Optional<Investigation> findByIndemnisationRequestId(String indemnisationRequestId);
}
