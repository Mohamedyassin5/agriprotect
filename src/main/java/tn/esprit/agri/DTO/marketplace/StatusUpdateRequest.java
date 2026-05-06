package tn.esprit.agri.DTO.marketplace;

import lombok.Data;
import tn.esprit.agri.entities.enums.OrderStatus;

import java.time.LocalDate;

@Data
public class StatusUpdateRequest {
    private OrderStatus status;
    private String note;
    private String location;
    private LocalDate estimatedDelivery;
}
