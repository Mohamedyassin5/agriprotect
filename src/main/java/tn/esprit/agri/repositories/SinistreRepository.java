package tn.esprit.agri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.agri.entities.Sinistre;
import tn.esprit.agri.entities.enums.StatutSinistre;

import java.util.List;

@Repository
public interface SinistreRepository extends JpaRepository<Sinistre, String> {

    List<Sinistre> findByUserId(String userId);

    List<Sinistre> findByCropId(String cropId);

    List<Sinistre> findByStatut(StatutSinistre statut);

    @Query("SELECT s FROM Sinistre s WHERE s.user.id = :userId AND s.isResolved = false")
    List<Sinistre> findUnresolvedByUserId(@Param("userId") String userId);

}
