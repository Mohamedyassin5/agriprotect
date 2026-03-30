package tn.esprit.agri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.agri.entities.SolidarityFund;

import java.util.Optional;

public interface SolidarityFundRepository extends JpaRepository<SolidarityFund, String> {

    // Check if a fund exists with the same name and culture type
    Optional<SolidarityFund> findByNameAndCultureType(String name, String cultureType);

    // Optional: find by numeroFond if needed
    SolidarityFund findByNumeroFond(String numeroFond);
}
