package tn.esprit.agri.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.agri.DTO.marketplace.*;
import tn.esprit.agri.entities.*;
import tn.esprit.agri.entities.enums.*;
import tn.esprit.agri.repositories.*;
import tn.esprit.agri.services.IMarketplaceService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MarketplaceServiceImpl implements IMarketplaceService {

    private final ListingRepository listingRepository;
    private final MarketplaceOrderRepository orderRepository;
    private final MarketplaceReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final OrderTrackingEventRepository trackingEventRepository;

    // ── Listings ──────────────────────────────────────────────────

    @Override
    public ListingResponse createListing(String sellerId, ListingRequest req) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Listing listing = Listing.builder()
                .seller(seller)
                .title(req.getTitle())
                .description(req.getDescription())
                .category(req.getCategory())
                .type(req.getType())
                .price(req.getPrice())
                .unit(req.getUnit())
                .stock(req.getStock() != null ? req.getStock() : 1)
                .images(req.getImages() != null ? req.getImages() : new ArrayList<>())
                .location(req.getLocation())
                .status(ListingStatus.PENDING)
                .build();
        return toListingResponse(listingRepository.save(listing));
    }

    @Override
    @Transactional(readOnly = true)
    public ListingResponse getListing(Long id) {
        Listing l = listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing not found"));
        l.setViewCount(l.getViewCount() + 1);
        listingRepository.save(l);
        return toListingResponse(l);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ListingResponse> searchListings(ListingCategory category, ListingType type,
            BigDecimal minPrice, BigDecimal maxPrice, String search, Pageable pageable) {
        return listingRepository.search(category, type, minPrice, maxPrice, search, pageable)
                .map(this::toListingResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListingResponse> getMyListings(String sellerId) {
        return listingRepository.findBySellerId(sellerId).stream()
                .map(this::toListingResponse).collect(Collectors.toList());
    }

    @Override
    public ListingResponse updateListing(Long id, String sellerId, ListingRequest req) {
        Listing l = listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing not found"));
        if (!l.getSeller().getId().equals(sellerId))
            throw new RuntimeException("Not authorized");
        l.setTitle(req.getTitle());
        l.setDescription(req.getDescription());
        l.setCategory(req.getCategory());
        l.setType(req.getType());
        l.setPrice(req.getPrice());
        l.setUnit(req.getUnit());
        if (req.getStock() != null) l.setStock(req.getStock());
        if (req.getImages() != null) l.setImages(req.getImages());
        if (req.getLocation() != null) l.setLocation(req.getLocation());
        return toListingResponse(listingRepository.save(l));
    }

    @Override
    public void closeListing(Long id, String sellerId) {
        Listing l = listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing not found"));
        if (!l.getSeller().getId().equals(sellerId))
            throw new RuntimeException("Not authorized");
        l.setStatus(ListingStatus.CLOSED);
        listingRepository.save(l);
    }

    // ── Admin ─────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ListingResponse> getPendingListings() {
        return listingRepository.findByStatusOrderByCreatedAtDesc(ListingStatus.PENDING).stream()
                .map(this::toListingResponse).collect(Collectors.toList());
    }

    @Override
    public ListingResponse approveListing(Long id, String adminNote) {
        Listing l = listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing not found"));
        l.setStatus(ListingStatus.ACTIVE);
        l.setAdminNote(adminNote);
        return toListingResponse(listingRepository.save(l));
    }

    @Override
    public ListingResponse rejectListing(Long id, String adminNote) {
        Listing l = listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing not found"));
        l.setStatus(ListingStatus.REJECTED);
        l.setAdminNote(adminNote);
        return toListingResponse(listingRepository.save(l));
    }

    @Override
    @Transactional(readOnly = true)
    public MarketplaceAdminOverview getAdminOverview() {
        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (ListingCategory c : ListingCategory.values()) {
            byCategory.put(c.name(), listingRepository.findByStatus(ListingStatus.ACTIVE).stream()
                    .filter(l -> l.getCategory() == c).count());
        }
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (OrderStatus s : OrderStatus.values()) {
            byStatus.put(s.name(), orderRepository.countByStatus(s));
        }
        return MarketplaceAdminOverview.builder()
                .totalListings(listingRepository.count())
                .pendingListings(listingRepository.countByStatus(ListingStatus.PENDING))
                .activeListings(listingRepository.countByStatus(ListingStatus.ACTIVE))
                .totalOrders(orderRepository.count())
                .pendingOrders(orderRepository.countByStatus(OrderStatus.PENDING))
                .deliveredOrders(orderRepository.countByStatus(OrderStatus.DELIVERED))
                .totalRevenue(orderRepository.totalPlatformRevenue())
                .listingsByCategory(byCategory)
                .ordersByStatus(byStatus)
                .build();
    }

    // ── Orders ────────────────────────────────────────────────────

    @Override
    public OrderResponse placeOrder(String buyerId, OrderRequest req) {
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Listing listing = listingRepository.findById(req.getListingId())
                .orElseThrow(() -> new RuntimeException("Listing not found"));
        if (listing.getStatus() != ListingStatus.ACTIVE)
            throw new RuntimeException("Listing is not available");
        if (listing.getSeller().getId().equals(buyerId))
            throw new RuntimeException("Cannot buy your own listing");
        int qty = req.getQuantity() != null ? req.getQuantity() : 1;
        if (listing.getStock() < qty)
            throw new RuntimeException("Insufficient stock");

        BigDecimal total = listing.getPrice().multiply(BigDecimal.valueOf(qty));

        MarketplaceOrder order = MarketplaceOrder.builder()
                .buyer(buyer)
                .listing(listing)
                .quantity(qty)
                .unitPrice(listing.getPrice())
                .totalPrice(total)
                .paymentSource(req.getPaymentSource())
                .deliveryAddress(req.getDeliveryAddress())
                .notes(req.getNotes())
                .status(OrderStatus.PENDING)
                .build();

        listing.setStock(listing.getStock() - qty);
        if (listing.getStock() == 0) listing.setStatus(ListingStatus.SOLD);
        listingRepository.save(listing);

        MarketplaceOrder saved = orderRepository.save(order);
        trackingEventRepository.save(OrderTrackingEvent.builder()
                .order(saved)
                .status(OrderStatus.PENDING)
                .description("Commande reçue et en attente de confirmation du vendeur")
                .location("Plateforme AgriProtect")
                .build());
        return toOrderResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(String buyerId) {
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId).stream()
                .map(this::toOrderResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getMySales(String sellerId) {
        return orderRepository.findByListingSellerIdOrderByCreatedAtDesc(sellerId).stream()
                .map(this::toOrderResponse).collect(Collectors.toList());
    }

    @Override
    public OrderResponse updateOrderStatus(Long orderId, String userId, StatusUpdateRequest req) {
        MarketplaceOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (userId != null && !order.getListing().getSeller().getId().equals(userId))
            throw new RuntimeException("Non autorisé : seul le vendeur peut modifier cette commande");
        if (req.getStatus() == OrderStatus.CONFIRMED && order.getTrackingNumber() == null)
            order.setTrackingNumber("AGR-" + LocalDate.now().getYear()
                    + String.format("%06d", (int)(Math.random() * 1_000_000)));
        if (req.getEstimatedDelivery() != null)
            order.setEstimatedDelivery(req.getEstimatedDelivery());
        if (req.getNote() != null && !req.getNote().isBlank())
            order.setSellerNote(req.getNote());
        order.setStatus(req.getStatus());
        MarketplaceOrder saved = orderRepository.save(order);
        String description = (req.getNote() != null && !req.getNote().isBlank())
                ? req.getNote() : defaultTrackingDescription(req.getStatus());
        String location = (req.getLocation() != null && !req.getLocation().isBlank())
                ? req.getLocation() : "Tunisie";
        trackingEventRepository.save(OrderTrackingEvent.builder()
                .order(saved).status(req.getStatus())
                .description(description).location(location).build());
        return toOrderResponse(saved);
    }

    private String defaultTrackingDescription(OrderStatus status) {
        return switch (status) {
            case CONFIRMED        -> "Commande confirmée par le vendeur";
            case SHIPPED          -> "Colis pris en charge par le service de livraison";
            case IN_TRANSIT       -> "Colis en transit vers la destination";
            case OUT_FOR_DELIVERY -> "Colis en cours de livraison finale";
            case DELIVERED        -> "Colis livré avec succès";
            case CANCELLED        -> "Commande annulée";
            case REFUNDED         -> "Remboursement effectué";
            default               -> "Statut mis à jour";
        };
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .sorted(Comparator.comparing(MarketplaceOrder::getCreatedAt).reversed())
                .map(this::toOrderResponse).collect(Collectors.toList());
    }

    // ── Reviews ───────────────────────────────────────────────────

    @Override
    public ReviewResponse addReview(String authorId, ReviewRequest req) {
        if (reviewRepository.existsByOrderId(req.getOrderId()))
            throw new RuntimeException("Review already exists for this order");
        MarketplaceOrder order = orderRepository.findById(req.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (!order.getBuyer().getId().equals(authorId))
            throw new RuntimeException("Not authorized");
        if (order.getStatus() != OrderStatus.DELIVERED)
            throw new RuntimeException("Can only review delivered orders");
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        MarketplaceReview review = MarketplaceReview.builder()
                .order(order)
                .author(author)
                .listing(order.getListing())
                .rating(req.getRating())
                .comment(req.getComment())
                .build();
        reviewRepository.save(review);

        // Update listing rating
        Listing listing = order.getListing();
        Double avg = reviewRepository.averageRatingForListing(listing.getId());
        long count = reviewRepository.countByListingId(listing.getId());
        listing.setAverageRating(avg);
        listing.setReviewCount((int) count);
        listingRepository.save(listing);

        return toReviewResponse(review);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getListingReviews(Long listingId) {
        return reviewRepository.findByListingIdOrderByCreatedAtDesc(listingId).stream()
                .map(this::toReviewResponse).collect(Collectors.toList());
    }

    // ── Mappers ───────────────────────────────────────────────────

    private ListingResponse toListingResponse(Listing l) {
        return ListingResponse.builder()
                .id(l.getId())
                .sellerId(l.getSeller().getId())
                .sellerName(l.getSeller().getFirstName() + " " + l.getSeller().getLastName())
                .title(l.getTitle())
                .description(l.getDescription())
                .category(l.getCategory())
                .type(l.getType())
                .price(l.getPrice())
                .unit(l.getUnit())
                .stock(l.getStock())
                .images(l.getImages())
                .status(l.getStatus())
                .location(l.getLocation())
                .adminNote(l.getAdminNote())
                .viewCount(l.getViewCount())
                .averageRating(l.getAverageRating())
                .reviewCount(l.getReviewCount())
                .createdAt(l.getCreatedAt())
                .build();
    }

    private OrderResponse toOrderResponse(MarketplaceOrder o) {
        User buyer  = o.getBuyer();
        User seller = o.getListing().getSeller();
        String buyerName  = fullName(buyer.getFirstName(), buyer.getLastName(), buyer.getEmail());
        String sellerName = fullName(seller.getFirstName(), seller.getLastName(), seller.getEmail());
        List<OrderTrackingEventResponse> events = trackingEventRepository
                .findByOrderIdOrderByCreatedAtAsc(o.getId()).stream()
                .map(e -> OrderTrackingEventResponse.builder()
                        .id(e.getId()).status(e.getStatus())
                        .description(e.getDescription()).location(e.getLocation())
                        .createdAt(e.getCreatedAt()).build())
                .collect(Collectors.toList());
        String firstImage = (o.getListing().getImages() != null && !o.getListing().getImages().isEmpty())
                ? o.getListing().getImages().get(0) : null;
        return OrderResponse.builder()
                .id(o.getId())
                .buyerId(buyer.getId()).buyerName(buyerName).buyerEmail(buyer.getEmail())
                .listingId(o.getListing().getId()).listingTitle(o.getListing().getTitle())
                .listingImageUrl(firstImage)
                .sellerName(sellerName).sellerEmail(seller.getEmail()).sellerId(seller.getId())
                .quantity(o.getQuantity()).unitPrice(o.getUnitPrice()).totalPrice(o.getTotalPrice())
                .status(o.getStatus()).paymentSource(o.getPaymentSource())
                .deliveryAddress(o.getDeliveryAddress()).notes(o.getNotes())
                .sellerNote(o.getSellerNote()).trackingNumber(o.getTrackingNumber())
                .estimatedDelivery(o.getEstimatedDelivery())
                .createdAt(o.getCreatedAt())
                .hasReview(reviewRepository.existsByOrderId(o.getId()))
                .trackingEvents(events)
                .build();
    }

    private String fullName(String first, String last, String fallback) {
        String n = ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
        return n.isEmpty() ? fallback : n;
    }

    private ReviewResponse toReviewResponse(MarketplaceReview r) {
        return ReviewResponse.builder()
                .id(r.getId())
                .orderId(r.getOrder().getId())
                .authorName(r.getAuthor().getFirstName() + " " + r.getAuthor().getLastName())
                .listingId(r.getListing().getId())
                .rating(r.getRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
