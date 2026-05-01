package tn.esprit.agri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.entities.enums.Role;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    
    List<User> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            String firstName,
            String lastName
    );

    // Added for SolidarityFund segmentation and Investigation expert lookup
    List<User> findByRole(Role role);

    java.util.Optional<User> findFirstByRoleAndExpertFundId(Role role, String expertFundId);
}
