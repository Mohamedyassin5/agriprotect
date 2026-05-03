package tn.esprit.agri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.agri.entities.MarketplaceOrder;
import tn.esprit.agri.entities.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface MarketplaceOrderRepository extends JpaRepository<MarketplaceOrder, Long> {
    List<MarketplaceOrder> findByBuyerIdOrderByCreatedAtDesc(String buyerId);
    List<MarketplaceOrder> findByListingSellerIdOrderByCreatedAtDesc(String sellerId);
    List<MarketplaceOrder> findByStatus(OrderStatus status);
    boolean existsByBuyerIdAndListingId(String buyerId, Long listingId);

    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM MarketplaceOrder o WHERE o.listing.seller.id = :sellerId AND o.status = 'DELIVERED'")
    BigDecimal totalRevenueForSeller(@Param("sellerId") String sellerId);

    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM MarketplaceOrder o WHERE o.status = 'DELIVERED'")
    BigDecimal totalPlatformRevenue();

    long countByStatus(OrderStatus status);

    @Query("SELECT o FROM MarketplaceOrder o WHERE o.status IN :statuses AND o.estimatedDelivery IS NOT NULL AND o.estimatedDelivery <= :today")
    List<MarketplaceOrder> findAutoAdvanceEligible(@Param("statuses") List<OrderStatus> statuses, @Param("today") LocalDate today);
}
