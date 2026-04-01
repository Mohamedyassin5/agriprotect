package tn.esprit.agri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.agri.entities.Crop;

import java.util.List;

public interface CropRepository extends JpaRepository<Crop, String> {
    List<Crop> findByUserId(String userId);
    
    List<Crop> findByCropTypeContainingIgnoreCaseOrTypeterresContainingIgnoreCase(
            String cropType,
            String typeterres
    );
}
