package tn.esprit.agri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.agri.entities.Payment;
import tn.esprit.agri.entities.Insurance;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, String> {

    List<Payment> findByInsuranceOrderByPaymentDateDesc(Insurance insurance);

    List<Payment> findByInsuranceUserIdOrderByPaymentDateDesc(String userId); // Pour le farmer

    long countByStatus(String status);
}