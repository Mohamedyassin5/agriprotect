package tn.esprit.agri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.agri.entities.FarmerSolidarityFund;
import tn.esprit.agri.entities.FarmerSolidarityFundId;

import java.util.List;

public interface FarmerSolidarityFundRepository extends JpaRepository<FarmerSolidarityFund, FarmerSolidarityFundId> {

    List<FarmerSolidarityFund> findBySolidarityFundId(String fundId);

    List<FarmerSolidarityFund> findByFarmerId(String farmerId);

    java.util.Optional<FarmerSolidarityFund> findByFarmerIdAndSolidarityFundId(String farmerId,
            String solidarityFundId);
}