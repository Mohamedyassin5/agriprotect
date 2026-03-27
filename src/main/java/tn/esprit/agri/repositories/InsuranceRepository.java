package tn.esprit.agri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.agri.entities.Insurance;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.entities.enums.InsuranceStatus;

import java.util.List;
import java.util.Optional;

public interface InsuranceRepository extends JpaRepository<Insurance, String> {
    List<Insurance> findByUserAndStatus(User user, InsuranceStatus status);
    Optional<Insurance> findByIdAndUserId(String id, String userId);
}