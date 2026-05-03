package tn.esprit.agri.controlleurs;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import tn.esprit.agri.controlleurs.crop.dto.CropRequest;
import tn.esprit.agri.entities.Crop;
import tn.esprit.agri.services.ICropService;

import java.util.List;

@RestController
@RequestMapping("/agri/crops")
@RequiredArgsConstructor
public class CropController {

    private final ICropService cropService;

    @PostMapping("/user/{userId}")
    public ResponseEntity<Crop> createCropForUser(@PathVariable String userId, @Valid @RequestBody CropRequest req) {
        Crop crop = Crop.builder()
                .cropType(req.getCropType())
                .surface(req.getSurface())
                .optimalHumidity(req.getOptimalHumidity())
                .minHumidity(req.getMinHumidity())
                .maxHumidity(req.getMaxHumidity())
                .minTemperature(req.getMinTemperature())
                .maxTemperature(req.getMaxTemperature())
                .averageTemperature(req.getAverageTemperature())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .typeterres(req.getTypeterres())
                .build();

        Crop createdCrop = cropService.createCropForUser(userId, crop);
        return new ResponseEntity<>(createdCrop, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Crop> getCropById(@PathVariable String id) {
        try {
            Crop crop = cropService.getCropById(id);
            return ResponseEntity.ok(crop);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Crop>> getCropsByUser(@PathVariable String userId) {
        try {
            List<Crop> crops = cropService.getCropsByUser(userId);
            return ResponseEntity.ok(crops);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<Crop>> getAllCrops() {
        List<Crop> crops = cropService.getAllCrops();
        return ResponseEntity.ok(crops);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Crop> updateCrop(@PathVariable String id, @RequestBody Crop crop) {
        try {
            Crop updatedCrop = cropService.updateCrop(id, crop);
            return ResponseEntity.ok(updatedCrop);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCrop(@PathVariable String id) {
        try {
            cropService.deleteCrop(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<Crop>> searchCrops(@RequestParam String keyword) {
        List<Crop> crops = cropService.searchByKeyword(keyword);
        return ResponseEntity.ok(crops);
    }
}
