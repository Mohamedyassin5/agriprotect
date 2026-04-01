package tn.esprit.agri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.agri.entities.Echeance;
import tn.esprit.agri.entities.enums.StatutEcheance;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EcheanceRepository extends JpaRepository<Echeance, Long> {
    List<Echeance> findByCreditIdOrderByNumeroEcheanceAsc(Long creditId);
    List<Echeance> findByCreditIdAndStatutInOrderByNumeroEcheanceAsc(Long creditId, List<StatutEcheance> statuts);
    List<Echeance> findByDateEcheanceBeforeAndStatutIn(LocalDate date, List<StatutEcheance> statuts);
}
