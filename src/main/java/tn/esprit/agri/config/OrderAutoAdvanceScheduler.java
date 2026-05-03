package tn.esprit.agri.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.agri.entities.MarketplaceOrder;
import tn.esprit.agri.entities.OrderTrackingEvent;
import tn.esprit.agri.entities.enums.OrderStatus;
import tn.esprit.agri.repositories.MarketplaceOrderRepository;
import tn.esprit.agri.repositories.OrderTrackingEventRepository;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderAutoAdvanceScheduler {

    private final MarketplaceOrderRepository orderRepository;
    private final OrderTrackingEventRepository trackingEventRepository;

    /**
     * Runs every hour. Advances orders automatically based on estimatedDelivery:
     *  - IN_TRANSIT      + estimatedDelivery <= today  →  OUT_FOR_DELIVERY
     *  - OUT_FOR_DELIVERY + estimatedDelivery <= today  →  DELIVERED
     */
    @Scheduled(fixedRate = 3_600_000, initialDelay = 30_000)
    @Transactional
    public void autoAdvanceOrders() {
        LocalDate today = LocalDate.now();
        List<MarketplaceOrder> eligible = orderRepository.findAutoAdvanceEligible(
                List.of(OrderStatus.IN_TRANSIT, OrderStatus.OUT_FOR_DELIVERY), today);

        if (eligible.isEmpty()) return;

        log.info("[AutoAdvance] {} commande(s) éligible(s) au changement automatique de statut", eligible.size());

        for (MarketplaceOrder order : eligible) {
            OrderStatus prev = order.getStatus();
            OrderStatus next = prev == OrderStatus.IN_TRANSIT
                    ? OrderStatus.OUT_FOR_DELIVERY
                    : OrderStatus.DELIVERED;

            String description = next == OrderStatus.OUT_FOR_DELIVERY
                    ? "Colis arrivé en centre de distribution locale — livraison aujourd'hui"
                    : "Colis livré automatiquement à la date prévue";

            order.setStatus(next);
            orderRepository.save(order);

            trackingEventRepository.save(OrderTrackingEvent.builder()
                    .order(order)
                    .status(next)
                    .description(description)
                    .location("Tunisie")
                    .build());

            log.info("[AutoAdvance] Commande #{} : {} → {}", order.getId(), prev, next);
        }
    }
}
