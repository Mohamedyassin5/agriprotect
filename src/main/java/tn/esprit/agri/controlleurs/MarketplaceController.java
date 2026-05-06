package tn.esprit.agri.controlleurs;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.esprit.agri.DTO.marketplace.*;
import tn.esprit.agri.repositories.UserRepository;
import tn.esprit.agri.services.IMarketplaceService;
import tn.esprit.agri.entities.enums.ListingCategory;
import tn.esprit.agri.entities.enums.ListingType;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/marketplace")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MarketplaceController {

    private final IMarketplaceService marketplaceService;
    private final UserRepository userRepository;

    // ── Browse ────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<Page<ListingResponse>> search(
            @RequestParam(required = false) ListingCategory category,
            @RequestParam(required = false) ListingType type,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(marketplaceService.searchListings(category, type, minPrice, maxPrice, search,
                PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ListingResponse> getListing(@PathVariable Long id) {
        return ResponseEntity.ok(marketplaceService.getListing(id));
    }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<List<ReviewResponse>> getReviews(@PathVariable Long id) {
        return ResponseEntity.ok(marketplaceService.getListingReviews(id));
    }

    // ── Seller ────────────────────────────────────────────────────
    @PostMapping("/listings")
    public ResponseEntity<ListingResponse> create(@RequestBody ListingRequest req, Authentication auth) {
        String userId = getUserId(auth);
        return ResponseEntity.ok(marketplaceService.createListing(userId, req));
    }

    @GetMapping("/my-listings")
    public ResponseEntity<List<ListingResponse>> myListings(Authentication auth) {
        return ResponseEntity.ok(marketplaceService.getMyListings(getUserId(auth)));
    }

    @PutMapping("/listings/{id}")
    public ResponseEntity<ListingResponse> update(@PathVariable Long id, @RequestBody ListingRequest req, Authentication auth) {
        return ResponseEntity.ok(marketplaceService.updateListing(id, getUserId(auth), req));
    }

    @DeleteMapping("/listings/{id}")
    public ResponseEntity<Void> close(@PathVariable Long id, Authentication auth) {
        marketplaceService.closeListing(id, getUserId(auth));
        return ResponseEntity.noContent().build();
    }

    // ── Orders ────────────────────────────────────────────────────
    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> placeOrder(@RequestBody OrderRequest req, Authentication auth) {
        return ResponseEntity.ok(marketplaceService.placeOrder(getUserId(auth), req));
    }

    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderResponse>> myOrders(Authentication auth) {
        return ResponseEntity.ok(marketplaceService.getMyOrders(getUserId(auth)));
    }

    @GetMapping("/my-sales")
    public ResponseEntity<List<OrderResponse>> mySales(Authentication auth) {
        return ResponseEntity.ok(marketplaceService.getMySales(getUserId(auth)));
    }

    @PatchMapping("/orders/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(@PathVariable Long id,
            @RequestBody StatusUpdateRequest req, Authentication auth) {
        return ResponseEntity.ok(marketplaceService.updateOrderStatus(id, getUserId(auth), req));
    }

    // ── Reviews ───────────────────────────────────────────────────
    @PostMapping("/reviews")
    public ResponseEntity<ReviewResponse> addReview(@RequestBody ReviewRequest req, Authentication auth) {
        return ResponseEntity.ok(marketplaceService.addReview(getUserId(auth), req));
    }

    private String getUserId(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }
}
