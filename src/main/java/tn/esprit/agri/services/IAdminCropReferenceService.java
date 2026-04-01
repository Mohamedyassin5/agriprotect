package tn.esprit.agri.services;

import tn.esprit.agri.entities.CropReference;

public interface IAdminCropReferenceService {

    CropReference addReference(CropReference ref);
    CropReference updateReference(String id, CropReference updated);
}