package tn.esprit.agri.controlleurs;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.agri.DTO.marketplace.*;
import tn.esprit.agri.services.IMarketplaceService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/marketplace")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class MarketplaceAdminController {

    private final IMarketplaceService marketplaceService;

    @GetMapping("/overview")
    public ResponseEntity<MarketplaceAdminOverview> overview() {
        return ResponseEntity.ok(marketplaceService.getAdminOverview());
    }

    @GetMapping("/pending")
    public ResponseEntity<List<ListingResponse>> pending() {
        return ResponseEntity.ok(marketplaceService.getPendingListings());
    }

    @PostMapping("/listings/{id}/approve")
    public ResponseEntity<ListingResponse> approve(@PathVariable Long id,
            @RequestParam(required = false, defaultValue = "") String note) {
        return ResponseEntity.ok(marketplaceService.approveListing(id, note));
    }

    @PostMapping("/listings/{id}/reject")
    public ResponseEntity<ListingResponse> reject(@PathVariable Long id,
            @RequestParam(required = false, defaultValue = "") String note) {
        return ResponseEntity.ok(marketplaceService.rejectListing(id, note));
    }

    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponse>> allOrders() {
        return ResponseEntity.ok(marketplaceService.getAllOrders());
    }

    @PatchMapping("/orders/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(@PathVariable Long id,
            @RequestBody StatusUpdateRequest req) {
        return ResponseEntity.ok(marketplaceService.updateOrderStatus(id, null, req));
    }
}
