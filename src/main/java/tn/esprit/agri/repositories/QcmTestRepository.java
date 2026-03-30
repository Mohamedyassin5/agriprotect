package tn.esprit.agri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.agri.entities.QcmTest;

import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface QcmTestRepository extends JpaRepository<QcmTest, String> {
    List<QcmTest> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDateTime start, LocalDateTime end);
}