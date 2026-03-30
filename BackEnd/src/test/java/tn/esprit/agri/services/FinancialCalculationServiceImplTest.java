package tn.esprit.agri.services;

import org.junit.jupiter.api.Test;
import tn.esprit.agri.DTO.CreditDTO.CreditSimulationResultDto;
import tn.esprit.agri.services.impl.FinancialCalculationServiceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinancialCalculationServiceImplTest {

    private final FinancialCalculationServiceImpl service = new FinancialCalculationServiceImpl();

    @Test
    void calculateMensualite_shouldHandleZeroRate() {
        double mensualite = service.calculateMensualite(1200.0, 0.0, 12);
        assertEquals(100.0, mensualite);
    }

    @Test
    void simulate_shouldReturnConsistentTotals() {
        CreditSimulationResultDto result = service.simulate(10000.0, 12.0, 12, 100.0);
        assertTrue(result.getMensualite() > 0);
        assertTrue(result.getTotalInterets() >= 0);
        assertTrue(result.getTotalCost() >= 10100.0);
    }

    @Test
    void calculatePenalty_shouldIncreaseWithDelay() {
        double p1 = service.calculatePenalty(1000, 5);
        double p2 = service.calculatePenalty(1000, 10);
        assertTrue(p2 > p1);
    }
}
