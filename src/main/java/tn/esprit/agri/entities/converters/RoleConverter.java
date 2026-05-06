package tn.esprit.agri.entities.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tn.esprit.agri.entities.enums.Role;

@Converter(autoApply = false)
public class RoleConverter implements AttributeConverter<Role, String> {

    @Override
    public String convertToDatabaseColumn(Role role) {
        if (role == null) {
            return null;
        }
        return role.name();
    }

    @Override
    public Role convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return Role.FARMER; // Default
        }
        try {
            return Role.valueOf(dbData.trim());
        } catch (IllegalArgumentException e) {
            System.err.println("WARNING: Unknown role in database: [" + dbData + "]. Defaulting to FARMER.");
            return Role.FARMER;
        }
    }
}
