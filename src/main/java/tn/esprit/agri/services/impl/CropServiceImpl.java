package tn.esprit.agri.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.agri.entities.Crop;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.repositories.CropRepository;
import tn.esprit.agri.repositories.UserRepository;
import tn.esprit.agri.services.ICropService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CropServiceImpl implements ICropService {

    private final CropRepository cropRepository;
    private final UserRepository userRepository;

    @Override
    public Crop createCropForUser(String userId, Crop crop) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        crop.setUser(user);
        crop.setCreatedAt(LocalDateTime.now());
        return cropRepository.save(crop);
    }

    @Override
    public Crop getCropById(String cropId) {
        return cropRepository.findById(cropId)
                .orElseThrow(() -> new RuntimeException("Crop not found with id: " + cropId));
    }

    @Override
    public List<Crop> getCropsByUser(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found with id: " + userId);
        }
        return cropRepository.findByUserId(userId);
    }

    @Override
    public List<Crop> getAllCrops() {
        return cropRepository.findAll();
    }

    @Override
    public Crop updateCrop(String cropId, Crop cropDetails) {
        Crop existingCrop = getCropById(cropId);
        existingCrop.setCropType(cropDetails.getCropType());
        existingCrop.setSurface(cropDetails.getSurface());
        existingCrop.setOptimalHumidity(cropDetails.getOptimalHumidity());
        existingCrop.setMinHumidity(cropDetails.getMinHumidity());
        existingCrop.setMaxHumidity(cropDetails.getMaxHumidity());
        existingCrop.setMinTemperature(cropDetails.getMinTemperature());
        existingCrop.setMaxTemperature(cropDetails.getMaxTemperature());
        existingCrop.setAverageTemperature(cropDetails.getAverageTemperature());
        existingCrop.setStartDate(cropDetails.getStartDate());
        existingCrop.setEndDate(cropDetails.getEndDate());
        existingCrop.setTypeterres(cropDetails.getTypeterres());
        return cropRepository.save(existingCrop);
    }

    @Override
    public void deleteCrop(String cropId) {
        if (!cropRepository.existsById(cropId)) {
            throw new RuntimeException("Crop not found with id: " + cropId);
        }
        cropRepository.deleteById(cropId);
    }
}
