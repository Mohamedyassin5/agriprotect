package tn.esprit.agri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.agri.entities.FarmerQcmResult;

@Repository
public interface FarmerQcmResultRepository extends JpaRepository<FarmerQcmResult, String> {
    FarmerQcmResult findTopByFarmerIdOrderByCompletedAtDesc(String farmerId);

    FarmerQcmResult findTopByFarmerIdAndTestIdOrderByCompletedAtDesc(String farmerId, String testId);

    boolean existsByFarmerIdAndTestIdAndPassedTrue(String farmerId, String testId);
}