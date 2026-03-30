package tn.esprit.agri.controlleurs.crop;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.agri.ai.dto.CropAiRequest;
import tn.esprit.agri.ai.dto.CropAiResponse;
import tn.esprit.agri.ai.service.CropRecommendationService;

@RestController @RequestMapping("/agri/crops")
@RequiredArgsConstructor
public class CropRecommendationController {

    private final CropRecommendationService cropRecommendationService;

    @PostMapping("/recommend")
    public ResponseEntity<CropAiResponse> recommend(@RequestBody @Valid CropAiRequest request) {
        return ResponseEntity.ok(cropRecommendationService.recommend(request));
    }
}
