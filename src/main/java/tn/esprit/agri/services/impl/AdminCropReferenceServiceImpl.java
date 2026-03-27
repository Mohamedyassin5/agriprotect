package tn.esprit.agri.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.agri.entities.CropReference;
import tn.esprit.agri.repositories.CropReferenceRepository;
import tn.esprit.agri.services.IAdminCropReferenceService;

@Service
@RequiredArgsConstructor
public class AdminCropReferenceServiceImpl implements IAdminCropReferenceService {

    private final CropReferenceRepository repository;

    @Override
    public CropReference addReference(CropReference ref) {
        // Validation simple : pas de doublon pour même cropType + year
        repository.findByCropTypeAndReferenceYear(ref.getCropType(), ref.getReferenceYear())
                .ifPresent(existing -> { throw new RuntimeException("Référence déjà existante pour cette année"); });
        return repository.save(ref);
    }

    @Override
    public CropReference updateReference(String id, CropReference updated) {
        return repository.findById(id)
                .map(existing -> {
                    existing.setReferenceYield(updated.getReferenceYield());
                    existing.setReferencePrice(updated.getReferencePrice());
                    existing.setBasePremiumRate(updated.getBasePremiumRate());
                    existing.setReferenceYear(updated.getReferenceYear());
                    return repository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("Référence non trouvée"));
    }
}