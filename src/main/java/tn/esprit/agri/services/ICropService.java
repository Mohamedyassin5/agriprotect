package tn.esprit.agri.services;

import tn.esprit.agri.entities.Crop;

import java.util.List;

public interface ICropService {
    Crop createCropForUser(String userId, Crop crop);
    Crop getCropById(String cropId);
    List<Crop> getCropsByUser(String userId);
    List<Crop> getAllCrops();
    Crop updateCrop(String cropId, Crop cropDetails);
    void deleteCrop(String cropId);
    List<Crop> searchByKeyword(String keyword);
}
