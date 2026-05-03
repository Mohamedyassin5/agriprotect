package tn.esprit.agri.repositories;
 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.agri.entities.Risque;
import tn.esprit.agri.entities.enums.RiskType;
import tn.esprit.agri.entities.enums.Severity;
 
import java.time.LocalDateTime;
import java.util.List;
 
@Repository
public interface RisqueRepository extends JpaRepository<Risque, String> {
 
    List<Risque> findByCropId(String cropId);
 
    List<Risque> findByUserId(String userId);
 
    List<Risque> findByIsResolved(Boolean isResolved);
 
    List<Risque> findByTypeSinistre(RiskType riskType);
 
    List<Risque> findBySeverity(Severity severity);
 
    @Query("SELECT r FROM Risque r WHERE r.crop.id = :cropId AND r.isResolved = false")
    List<Risque> findUnresolvedByCropId(@Param("cropId") String cropId);
 
    @Query("SELECT r FROM Risque r WHERE r.user.id = :userId AND r.isResolved = false")
    List<Risque> findUnresolvedByUserId(@Param("userId") String userId);
 
    @Query("SELECT r FROM Risque r WHERE r.detectedAt BETWEEN :startDate AND :endDate AND r.crop.id = :cropId")
    List<Risque> findRisquesBetweenDates(
            @Param("cropId") String cropId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
 
    @Query("SELECT COUNT(r) FROM Risque r WHERE r.typeSinistre = :riskType AND r.isResolved = false AND r.crop.id = :cropId")
    long countUnresolvedByType(@Param("riskType") RiskType riskType, @Param("cropId") String cropId);
}

