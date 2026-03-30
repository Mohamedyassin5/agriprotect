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
    List<User> findByRole(Role role);
    Optional<User> findFirstByRoleAndExpertFundId(Role role, String expertFundId);
}
