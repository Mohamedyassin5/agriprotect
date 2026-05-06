package tn.esprit.agri.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tn.esprit.agri.DTO.marketplace.*;
import tn.esprit.agri.entities.enums.ListingCategory;
import tn.esprit.agri.entities.enums.ListingType;

import java.math.BigDecimal;
import java.util.List;

public interface IMarketplaceService {
    // Listings
    ListingResponse createListing(String sellerId, ListingRequest request);
    ListingResponse getListing(Long id);
    Page<ListingResponse> searchListings(ListingCategory category, ListingType type, BigDecimal minPrice, BigDecimal maxPrice, String search, Pageable pageable);
    List<ListingResponse> getMyListings(String sellerId);
    ListingResponse updateListing(Long id, String sellerId, ListingRequest request);
    void closeListing(Long id, String sellerId);

    // Admin
    List<ListingResponse> getPendingListings();
    ListingResponse approveListing(Long id, String adminNote);
    ListingResponse rejectListing(Long id, String adminNote);
    MarketplaceAdminOverview getAdminOverview();

    // Orders
    OrderResponse placeOrder(String buyerId, OrderRequest request);
    List<OrderResponse> getMyOrders(String buyerId);
    List<OrderResponse> getMySales(String sellerId);
    OrderResponse updateOrderStatus(Long orderId, String userId, StatusUpdateRequest req);
    List<OrderResponse> getAllOrders();

    // Reviews
    ReviewResponse addReview(String authorId, ReviewRequest request);
    List<ReviewResponse> getListingReviews(Long listingId);
}
