package tn.esprit.agri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.agri.entities.CropReference;
import java.util.Optional;

public interface CropReferenceRepository extends JpaRepository<CropReference, String> {

    Optional<CropReference> findByCropTypeAndReferenceYear(String cropType, Integer year);
    Optional<CropReference> findTopByCropTypeOrderByReferenceYearDesc(String cropType);
}