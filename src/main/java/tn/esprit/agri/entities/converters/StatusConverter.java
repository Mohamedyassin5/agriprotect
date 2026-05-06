package tn.esprit.agri.entities.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tn.esprit.agri.entities.enums.Status;

@Converter(autoApply = false)
public class StatusConverter implements AttributeConverter<Status, String> {

    @Override
    public String convertToDatabaseColumn(Status status) {
        if (status == null) {
            return null;
        }
        return status.name();
    }

    @Override
    public Status convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return Status.ACTIVE; // Default
        }
        try {
            return Status.valueOf(dbData.trim());
        } catch (IllegalArgumentException e) {
            System.err.println("WARNING: Unknown status in database: [" + dbData + "]. Defaulting to ACTIVE.");
            return Status.ACTIVE;
        }
    }
}
