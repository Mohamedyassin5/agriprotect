package tn.esprit.agri.DTO.marketplace;

import lombok.Data;
import tn.esprit.agri.entities.enums.PaymentSource;

@Data
public class OrderRequest {
    private Long listingId;
    private Integer quantity;
    private PaymentSource paymentSource;
    private String deliveryAddress;
    private String notes;
}
