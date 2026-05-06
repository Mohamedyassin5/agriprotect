package tn.esprit.agri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import tn.esprit.agri.entities.DemandeCredit;
import tn.esprit.agri.entities.enums.StatutDemande;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DemandeCreditRepository extends JpaRepository<DemandeCredit, Long>, JpaSpecificationExecutor<DemandeCredit> {

    List<DemandeCredit> findByStatut(StatutDemande statut);

    List<DemandeCredit> findByAgriculteurId(String agriculteurId);

    List<DemandeCredit> findByDateDemandeBetween(LocalDate debut, LocalDate fin);

    List<DemandeCredit> findByStatutAndAgriculteurId(StatutDemande statut, String agriculteurId);
}
