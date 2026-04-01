package tn.esprit.agri.controlleurs;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.agri.entities.CropReference;
import tn.esprit.agri.repositories.CropReferenceRepository;
import tn.esprit.agri.services.IAdminCropReferenceService;
import tn.esprit.agri.services.ICropReferenceSyncService;

import java.util.List;

@RestController
@RequestMapping("/agri/admin/crop-references")
@RequiredArgsConstructor
public class AdminCropReferenceController {

    private final IAdminCropReferenceService service;
    private final CropReferenceRepository repository;
    private final ICropReferenceSyncService syncService;

    @PostMapping
    public ResponseEntity<CropReference> add(@RequestBody CropReference ref) {
        return ResponseEntity.status(201).body(service.addReference(ref));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CropReference> update(@PathVariable String id, @RequestBody CropReference updated) {
        return ResponseEntity.ok(service.updateReference(id, updated));
    }

    @GetMapping
    public ResponseEntity<List<CropReference>> getAll() {
        return ResponseEntity.ok(repository.findAll());
    }

    @PostMapping("/sync-agridata")
    public ResponseEntity<String> syncFromAgridata() {
        try {
            syncService.syncFromAgridataDashboards();
            return ResponseEntity.ok("✅ Données mises à jour depuis Agridata.tn avec succès !");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("❌ Erreur : " + e.getMessage());
        }
    }
}