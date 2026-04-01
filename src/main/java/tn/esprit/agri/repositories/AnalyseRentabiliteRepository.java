package tn.esprit.agri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.agri.entities.AnalyseRentabilite;

@Repository
public interface AnalyseRentabiliteRepository extends JpaRepository<AnalyseRentabilite, Long> {
}
