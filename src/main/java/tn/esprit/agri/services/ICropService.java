package tn.esprit.agri.services;

import tn.esprit.agri.entities.Crop;

import java.util.List;
import java.util.Map;

public interface ICropService {
    Crop createCropForUser(String userId, Crop crop);
    Crop getCropById(String cropId);
    List<Crop> getCropsByUser(String userId);
    List<Crop> getAllCrops();
    Crop updateCrop(String cropId, Crop cropDetails);
    void deleteCrop(String cropId);
    List<Crop> searchByKeyword(String keyword);
    Crop estimateCropValue(String cropId);
    private float estimateLandValue(String cropType, float surface) {

        Map<String, Float> pricePerHectare = Map.ofEntries(
                Map.entry("wheat", 5000f),
                Map.entry("barley", 4500f),
                Map.entry("olive", 8000f),
                Map.entry("dates", 15000f),
                Map.entry("citrus", 11000f),
                Map.entry("grapes", 9000f),
                Map.entry("almonds", 9500f),
                Map.entry("vegetables", 12000f),
                Map.entry("tomato", 13000f),
                Map.entry("potato", 10000f),
                Map.entry("pepper", 12500f),
                Map.entry("fruits", 10000f),
                Map.entry("apple", 10500f),
                Map.entry("peach", 9800f)
        );

        Map<String, Float> cropFactor = Map.ofEntries(
                Map.entry("wheat", 1.0f),
                Map.entry("barley", 0.9f),
                Map.entry("olive", 1.3f),
                Map.entry("dates", 1.8f),
                Map.entry("citrus", 1.4f),
                Map.entry("grapes", 1.2f),
                Map.entry("almonds", 1.3f),
                Map.entry("vegetables", 1.5f),
                Map.entry("tomato", 1.6f),
                Map.entry("potato", 1.2f),
                Map.entry("pepper", 1.5f),
                Map.entry("fruits", 1.4f),
                Map.entry("apple", 1.3f),
                Map.entry("peach", 1.2f)
        );

        String key = cropType.toLowerCase().trim();

        float basePrice = pricePerHectare.getOrDefault(key, 4000f);
        float factor = cropFactor.getOrDefault(key, 1.0f);

        return surface * basePrice * factor;
    }
}
