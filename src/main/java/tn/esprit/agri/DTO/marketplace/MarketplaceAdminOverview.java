package tn.esprit.agri.DTO.marketplace;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data @Builder
public class MarketplaceAdminOverview {
    private long totalListings;
    private long pendingListings;
    private long activeListings;
    private long totalOrders;
    private long pendingOrders;
    private long deliveredOrders;
    private BigDecimal totalRevenue;
    private Map<String, Long> listingsByCategory;
    private Map<String, Long> ordersByStatus;
}
