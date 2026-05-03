package tn.esprit.agri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.agri.entities.PasswordResetCode;

import java.util.Optional;

public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, String> {
    Optional<PasswordResetCode> findTopByEmailOrderByExpiresAtDesc(String email);
    void deleteByEmail(String email);
}
