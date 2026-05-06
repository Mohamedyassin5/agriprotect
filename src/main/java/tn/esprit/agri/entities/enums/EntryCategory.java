package tn.esprit.agri.entities.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EntryCategory {
    SALES,
    SEEDS,
    FERTILIZER,
    IRRIGATION,
    LABOR,
    TRANSPORT,
    EQUIPMENT,
    INSURANCE,
    LOAN_PAYMENT,
    OTHER;

    @JsonCreator
    public static EntryCategory fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return OTHER;
        }
        try {
            return EntryCategory.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return OTHER;
        }
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
