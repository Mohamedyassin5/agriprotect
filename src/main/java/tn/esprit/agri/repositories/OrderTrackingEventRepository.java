package tn.esprit.agri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.agri.entities.OrderTrackingEvent;

import java.util.List;

public interface OrderTrackingEventRepository extends JpaRepository<OrderTrackingEvent, Long> {
    List<OrderTrackingEvent> findByOrderIdOrderByCreatedAtAsc(Long orderId);
}
