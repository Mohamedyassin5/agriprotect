package tn.esprit.agri.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.agri.entities.Listing;
import tn.esprit.agri.entities.enums.ListingCategory;
import tn.esprit.agri.entities.enums.ListingStatus;
import tn.esprit.agri.entities.enums.ListingType;

import java.math.BigDecimal;
import java.util.List;

public interface ListingRepository extends JpaRepository<Listing, Long> {
    List<Listing> findByStatus(ListingStatus status);
    Page<Listing> findByStatus(ListingStatus status, Pageable pageable);
    List<Listing> findBySellerId(String sellerId);
    Page<Listing> findBySellerIdOrderByCreatedAtDesc(String sellerId, Pageable pageable);

    @Query("SELECT l FROM Listing l WHERE l.status = 'ACTIVE' " +
           "AND (:category IS NULL OR l.category = :category) " +
           "AND (:type IS NULL OR l.type = :type) " +
           "AND (:minPrice IS NULL OR l.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR l.price <= :maxPrice) " +
           "AND (:search IS NULL OR LOWER(l.title) LIKE LOWER(CONCAT('%',:search,'%')) OR LOWER(l.description) LIKE LOWER(CONCAT('%',:search,'%')))")
    Page<Listing> search(@Param("category") ListingCategory category,
                         @Param("type") ListingType type,
                         @Param("minPrice") BigDecimal minPrice,
                         @Param("maxPrice") BigDecimal maxPrice,
                         @Param("search") String search,
                         Pageable pageable);

    List<Listing> findByStatusOrderByCreatedAtDesc(ListingStatus status);
    long countByStatus(ListingStatus status);
    long countBySellerId(String sellerId);
}
