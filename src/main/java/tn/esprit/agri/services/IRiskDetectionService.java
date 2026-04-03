package tn.esprit.agri.services;

import tn.esprit.agri.entities.Crop;
import tn.esprit.agri.entities.Risque;
 
import java.util.List;
 
public interface IRiskDetectionService {
    List<Risque> detectRisks(Crop crop);
    List<Risque> detectAllCropsRisks();
    void checkAndCreateSinistre(Crop crop); // keeping this name for now or rename to checkAndCreateRisque? 
    // The user said "Toute la logique liée à l'API météo reste la même."
    // Let's rename it to checkAndCreateRisque to be consistent with the entity.
    void resolveSinistre(String risqueId);
}

