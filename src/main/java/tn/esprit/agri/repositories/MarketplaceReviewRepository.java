package tn.esprit.agri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.agri.entities.MarketplaceReview;

import java.util.List;
import java.util.Optional;

public interface MarketplaceReviewRepository extends JpaRepository<MarketplaceReview, Long> {
    List<MarketplaceReview> findByListingIdOrderByCreatedAtDesc(Long listingId);
    Optional<MarketplaceReview> findByOrderId(Long orderId);
    boolean existsByOrderId(Long orderId);

    @Query("SELECT AVG(r.rating) FROM MarketplaceReview r WHERE r.listing.id = :listingId")
    Double averageRatingForListing(@Param("listingId") Long listingId);

    @Query("SELECT COUNT(r) FROM MarketplaceReview r WHERE r.listing.id = :listingId")
    long countByListingId(@Param("listingId") Long listingId);
}
