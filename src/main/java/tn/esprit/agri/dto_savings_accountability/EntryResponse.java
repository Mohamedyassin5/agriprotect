package tn.esprit.agri.dto_savings_accountability;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.agri.entities.enums.EntryCategory;
import tn.esprit.agri.entities.enums.EntrySource;
import tn.esprit.agri.entities.enums.EntryType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntryResponse {

    private Long id;
    private String userId;
    private EntryType entryType;
    private EntryCategory category;
    private BigDecimal amount;
    private String description;
    private LocalDate entryDate;
    private EntrySource source;
    private Long sourceRefId;
    private LocalDateTime createdAt;
}
