package tn.esprit.agri.controlleurs;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.agri.entities.CropReference;
import tn.esprit.agri.repositories.CropReferenceRepository;
import tn.esprit.agri.services.IAdminCropReferenceService;

import java.util.List;

@RestController
@RequestMapping("/agri/admin/crop-references")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCropReferenceController {

    private final IAdminCropReferenceService service;
    private final CropReferenceRepository repository;  // Pour GET all

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
}