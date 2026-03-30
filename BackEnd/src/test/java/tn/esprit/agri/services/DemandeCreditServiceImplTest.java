package tn.esprit.agri.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.agri.DTO.DemandeDTO.DecisionFinaleDto;
import tn.esprit.agri.entities.DemandeCredit;
import tn.esprit.agri.entities.enums.DecisionCredit;
import tn.esprit.agri.entities.enums.StatutDemande;
import tn.esprit.agri.exceptions.BusinessRuleException;
import tn.esprit.agri.repositories.AnalyseRentabiliteRepository;
import tn.esprit.agri.repositories.CreditRepository;
import tn.esprit.agri.repositories.DemandeCreditRepository;
import tn.esprit.agri.services.impl.DemandeCreditServiceImpl;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemandeCreditServiceImplTest {

    @Mock
    private DemandeCreditRepository demandeCreditRepository;
    @Mock
    private AnalyseRentabiliteRepository analyseRentabiliteRepository;
    @Mock
    private CreditRepository creditRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private FinancialCalculationService financialCalculationService;

    private DemandeCreditServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DemandeCreditServiceImpl(
                demandeCreditRepository,
                analyseRentabiliteRepository,
                creditRepository,
                auditService,
                financialCalculationService
        );
    }

    @Test
    void startInstruction_shouldMoveFromNouvelleToEnCours() {
        DemandeCredit demande = DemandeCredit.builder()
                .id(1L)
                .dateDemande(LocalDate.now())
                .agriculteurId(10L)
                .montantDemande(1000.0)
                .statut(StatutDemande.NOUVELLE)
                .build();

        when(demandeCreditRepository.findById(1L)).thenReturn(Optional.of(demande));
        when(demandeCreditRepository.save(any(DemandeCredit.class))).thenAnswer(i -> i.getArgument(0));

        var response = service.startInstruction(1L, 99L);
        assertEquals(StatutDemande.EN_COURS_INSTRUCTION, response.getStatut());
    }

    @Test
    void archive_shouldFailWhenNotFinalized() {
        DemandeCredit demande = DemandeCredit.builder()
                .id(1L)
                .dateDemande(LocalDate.now())
                .agriculteurId(10L)
                .montantDemande(1000.0)
                .statut(StatutDemande.NOUVELLE)
                .build();
        when(demandeCreditRepository.findById(1L)).thenReturn(Optional.of(demande));
        assertThrows(BusinessRuleException.class, () -> service.archiveDemande(1L, 5L));
    }

    @Test
    void finalizeDecision_shouldAcceptDemande() {
        DemandeCredit demande = DemandeCredit.builder()
                .id(1L)
                .dateDemande(LocalDate.now())
                .agriculteurId(10L)
                .montantDemande(1000.0)
                .statut(StatutDemande.EN_COURS_INSTRUCTION)
                .build();
        when(demandeCreditRepository.findById(1L)).thenReturn(Optional.of(demande));
        when(demandeCreditRepository.save(any(DemandeCredit.class))).thenAnswer(i -> i.getArgument(0));

        DecisionFinaleDto dto = new DecisionFinaleDto();
        dto.setDecision(DecisionCredit.ACCEPTEE);
        dto.setActorId(77L);
        var response = service.finaliserDecision(1L, dto);

        assertEquals(StatutDemande.ACCEPTEE, response.getStatut());
    }
}
