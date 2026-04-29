package tn.esprit.agri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.agri.entities.FarmerQcmResult;

import java.util.List;

@Repository
public interface FarmerQcmResultRepository extends JpaRepository<FarmerQcmResult, String> {
    List<FarmerQcmResult> findByFarmerId(String farmerId);

    FarmerQcmResult findTopByFarmerIdOrderByCompletedAtDesc(String farmerId);

    FarmerQcmResult findTopByFarmerIdAndTestIdOrderByCompletedAtDesc(String farmerId, String testId);

    boolean existsByFarmerIdAndTestIdAndPassedTrue(String farmerId, String testId);
}