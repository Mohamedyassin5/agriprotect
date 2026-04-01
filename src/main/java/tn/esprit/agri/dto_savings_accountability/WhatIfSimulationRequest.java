package tn.esprit.agri.dto_savings_accountability;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.agri.entities.enums.EntryCategory;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatIfSimulationRequest {
    private Map<EntryCategory, Double> categoryChanges; // category -> % change (e.g., +20 or -15)
    private Double incomeChangePercent;                  // overall income % change
    private Integer simulationMonths;                    // how many months to simulate (default 3)
}
