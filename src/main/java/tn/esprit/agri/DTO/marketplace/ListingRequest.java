package tn.esprit.agri.DTO.marketplace;

import lombok.Data;
import tn.esprit.agri.entities.enums.ListingCategory;
import tn.esprit.agri.entities.enums.ListingType;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ListingRequest {
    private String title;
    private String description;
    private ListingCategory category;
    private ListingType type;
    private BigDecimal price;
    private String unit;
    private Integer stock;
    private List<String> images;
    private String location;
}
