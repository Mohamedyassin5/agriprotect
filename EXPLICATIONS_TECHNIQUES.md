# AgriProtect — Explications Techniques (IA & Aide à la Décision)

---

## 1. Score de Santé Financière
`GET /api/ai/accounting/health-score`

**Principe :** Score global 0–100 calculé à partir de 5 métriques pondérées sur les 3–6 derniers mois.

**Métriques et poids :**

| Métrique | Poids | Calcul |
|---|---|---|
| Ratio Dépenses/Revenus | 30% | `expenses / income` → score 100 si ≤50%, 15 si >100% |
| Régularité des Revenus | 20% | Nb de mois avec revenus sur les 3 derniers → `/3 × 100` |
| Diversification | 15% | Nb de catégories de revenus distinctes × 25 (max 100) |
| Taux d'Épargne | 20% | `(income - expenses) / income` → 100 si ≥30%, 10 si négatif |
| Tendance | 15% | Net 3 derniers mois vs 3 mois précédents → 85 si en hausse, 25 si en baisse |

**Score final :**
```
overall = ratio×0.30 + regularité×0.20 + diversification×0.15 + épargne×0.20 + tendance×0.15
```

**Niveaux :** EXCELLENT (≥80) / GOOD (≥65) / FAIR (≥50) / POOR (≥35) / CRITICAL (<35)

**Recommandations automatiques** générées pour chaque métrique dont le score est faible (<50–60).

---

## 2. Prévision des Dépenses par IA (WMA)
`GET /api/ai/accounting/forecast/expenses?months=3`

**Principe :** Weighted Moving Average (moyenne mobile pondérée) sur 7 mois, par catégorie.

**Fenêtre de données :** 6 mois passés + mois courant = 7 valeurs par catégorie.

**Poids croissants (mois récents = plus d'importance) :**
```
M-6  M-5  M-4  M-3  M-2  M-1  M0
 1    1    2    2    3    3    4
```
```
WMA = (val×1 + val×1 + val×2 + val×2 + val×3 + val×3 + val×4) / (1+1+2+2+3+3+4)
    = somme pondérée / 16
```

**Détection de tendance :**
```
firstHalf  = M-6 + M-5 + M-4
secondHalf = M-3 + M-2 + M-1 + M0

growthRate = (secondHalf - firstHalf) / firstHalf × 100

> +5%  → INCREASING
< -5%  → DECREASING
sinon → STABLE
```

**Confiance décroissante :**
- M+1 → 85% (HIGH_CONFIDENCE)
- M+2 → 75% (MEDIUM_CONFIDENCE)
- M+3 → 65% (LOW_CONFIDENCE)

**Différence avec cashflow/forecast :** La WMA donne plus d'importance aux mois récents et travaille catégorie par catégorie. `cashflow/forecast` fait une projection linéaire globale (revenus + dépenses ensemble).

---

## 3. Simulateur What-If
`POST /api/ai/accounting/simulate/what-if`

**Principe :** Simulation de scénarios "Et si..." — modifier les dépenses par catégorie (en %) et/ou les revenus, puis voir l'impact projeté sur N mois.

**Données source :** Moyennes réelles des 3 derniers mois depuis `accounting_entry`.

**Calcul par catégorie :**
```
currentAmount  = totalRéel3Mois / 3
simulatedAmount = currentAmount × (1 + changePercent/100)
```

**Projections mensuelles :** Le même revenu/dépense simulé est répété sur `simulationMonths` mois avec cumul.

**Verdict :**
- `POSITIVE` : revenu net simulé > revenu net actuel
- `NEGATIVE` : revenu net simulé < revenu net actuel
- `NEUTRAL` : pas de changement

---

## 4. Tendance de Rentabilité (Régression Linéaire)
`GET /api/ai/accounting/trends/profitability?months=6`

**Principe :** Régression linéaire sur le profit net mensuel pour détecter une tendance mathématique.

**Données :** Profit net de chaque mois = `income - expenses`, sur les N derniers mois.

**Régression linéaire (moindres carrés) :**
```
slope     = (n×ΣXY - ΣX×ΣY) / (n×ΣX² - (ΣX)²)
intercept = (ΣY - slope×ΣX) / n
```
- `slope > 0` → rentabilité croissante
- `slope < 0` → rentabilité déclinante

**R² (coefficient de détermination) :**
```
R² = 1 - (SS_résiduel / SS_total)
```
Mesure la fiabilité de la tendance : R²=1 → tendance parfaite, R²=0 → données aléatoires.

**Seuils de tendance :** slope > 50 → GROWING / slope < -50 → DECLINING / sinon → STABLE

**En plus :** meilleur mois, pire mois, taux de croissance global `(last - first) / first × 100`.

---

## 5. Catégorisation Intelligente (NLP)
`POST /api/ai/accounting/categorize`

**Principe :** NLP (Natural Language Processing) basé sur un système de mots-clés pondérés par catégorie.

**Dictionnaire de mots-clés par catégorie :**
```
SEEDS     → ["semence", "graine", "plantation", "plant", "semis", "bouture", ...]
FERTILIZER→ ["engrais", "compost", "npk", "phosphate", "azote", "fumier", ...]
EQUIPMENT → ["tracteur", "machine", "outil", "reparation", "maintenance", ...]
LABOR     → ["ouvrier", "salaire", "travailleur", "journalier", ...]
TRANSPORT → ["camion", "livraison", "carburant", "gasoil", "essence", ...]
... (10 catégories)
```

**Calcul du score par catégorie :**
```
Pour chaque mot-clé trouvé dans la description :
  → score += 2.0 si len(keyword) > 4  (mots longs = plus spécifiques)
  → score += 1.0 sinon

normalizedScore = (score / (nbKeywords × 2.0)) × 100
```

**Résultat :** Catégorie avec le score le plus élevé → suggestion + confidenceScore + tous les scores.

---

## 6. Alertes Budget Prédictives (Daily Burn Rate)
`GET /api/ai/accounting/budget/predictive-alerts`

**Principe :** Calcule le taux de consommation quotidien et projette si le budget sera dépassé avant la fin du mois.

**Calcul :**
```
daysPassed    = jours écoulés depuis le 1er du mois
dailyBurnRate = dépensesActuelles / daysPassed

projectedMonthEnd = dépensesActuelles + dailyBurnRate × joursRestants
projectedOverspend = projectedMonthEnd - budget

dateEpuisement = aujourd'hui + (budgetRestant / dailyBurnRate)
```

**Sévérité :**
- `CRITICAL` : budget déjà dépassé aujourd'hui
- `HIGH` : dépassement projeté > 30%
- `MEDIUM` : dépassement projeté entre 10–30%
- `LOW` : dépassement projeté < 10%

---

## 7. Prédiction d'Atteinte des Objectifs
`GET /api/ai/savings/goal/predict-achievement`

**Principe :** WMA sur les 7 derniers mois de transactions d'épargne pour prédire quand chaque goal sera atteint.

**Poids WMA (épargne nette mensuelle) :**
```
M-6  M-5  M-4  M-3  M-2  M-1  M0
1.0  1.0  1.5  1.5  2.0  2.0  2.5
```
```
avgMonthlySavings = somme(net_mois × poids) / somme(poids)
```

**Allocation selon le mode de chaque goal :**
```
CUSTOM_PERCENTAGE → monthlyContrib = avgSavings × (customPct / 100)
AUTO_PRIORITY     → monthlyContrib = solde résiduel (après CUSTOM), séquentiel P1→P2→P3
AUTO_PROPORTIONAL → monthlyContrib = résiduel × (targetAmount / totalProportional)
```

**Pour les goals AUTO_PRIORITY**, P2 ne commence que quand P1 est terminé → `monthsOffset` cumulé.

**Estimation :**
```
monthsToComplete = remaining / monthlyContrib  (arrondi au supérieur)
estimatedDate    = aujourd'hui + monthsToComplete + offset

si estimatedDate ≤ targetDate → ON_TRACK
sinon                          → AT_RISK
```

**Probabilité :** `min(95, 50 + (contrib/remaining) × 200)`

**4 scénarios globaux :** rythme actuel / +50% ambitieux / -25% prudent / objectif 12 mois.

---

## 8. Plan d'Épargne Intelligent (Saisonnalité)
`GET /api/ai/savings/plan/smart`

**Principe :** Plan mensuel personnalisé qui adapte les montants à épargner selon les patterns saisonniers historiques.

**Sources de données :**
- Table `savings_account` → `currentBalance` (solde actuel) et `goalAmount` (objectif global du compte)
- Table `accounting_entry` → revenus et dépenses réels des **12 derniers mois** (pour calculer les facteurs saisonniers)

**Étape 1 — Facteurs saisonniers (12 mois d'historique) :**
```
Pour chaque mois calendaire (Jan→Déc) de l'année passée :
  netMois = income - expenses
  facteur = netMois / moyenneAnnuelle
```
Ex : si avril l'an dernier avait des revenus élevés → facteur > 1 → épargne recommandée plus haute en avril prochain.

Le facteur est basé sur le **même mois calendaire de l'année passée** :
→ Avril 2026 utilise les données réelles d'Avril 2025.

**Étape 2 — Montant de base :**
```
remaining = goalAmount - currentBalance
planMonths = remaining / (avgDisposable × 20%)
recommendedMonthly = remaining / planMonths
```

**Étape 3 — Ajustement saisonnier par mois :**
```
adjustedSavings = recommendedMonthly × facteur
facteur clampé entre 0.5 et 2.0 (max ×2, min /2)
```

**Notes générées :**
- facteur > 1.3 → "Mois historiquement favorable — épargnez davantage"
- facteur > 0.9 → "Mois normal"
- facteur ≤ 0.9 → "Mois historiquement difficile — montant réduit"

**Limitation :** Si un mois n'a pas de données comptables, son facteur est neutre (1.0).
La réponse indique `monthsWithAccountingData` — combien de mois sur 12 avaient des données réelles.

---

## 9. Score de Risque d'un Retrait
`POST /api/ai/savings/withdrawal/risk-assessment`

**Principe :** Score de risque 1 (faible) à 10 (critique) basé sur 4 facteurs pondérés.

**Sources de données par facteur :**

| Facteur | Poids | Source DB | Calcul |
|---|---|---|---|
| Impact sur les objectifs | 35% | `savings_account` → `currentBalance`, `goalAmount` | Chute de progression (%) vers goalAmount après retrait |
| Couverture des dépenses | 25% | `accounting_entry` → dépenses réelles 3 derniers mois | Nb de mois de dépenses couverts par le solde restant |
| Tampon (% retiré) | 20% | `savings_account` → `currentBalance` | `amount / currentBalance × 100` |
| Historique 3 mois | 20% | `savings_transaction` → nb dépôts/retraits réels 3 mois | Ratio dépôts/retraits récents |

**Détail scores (1=faible risque, 10=risque élevé) :**
```
Objectifs  : chute >50% → 10, >30% → 8, >15% → 6, >5% → 4, ≤5% → 2
Couverture : <0 mois → 10, <1 mois → 8, <2 mois → 6, <3 mois → 4, ≥3 mois → 2
Tampon     : >80% → 10, >60% → 8, >40% → 6, >20% → 4, ≤20% → 2
Historique : dépôts > retraits×2 → 2 (sain), dépôts > retraits → 4, retraits > dépôts×2 → 9 (inquiétant)
```

**Exemple concret — retrait 1000 DT sur solde 2000 DT (dépenses réelles 1510 DT/mois) :**
```
Couverture : solde restant = 1000 DT → 1000/1510 = 0.66 mois (<1 mois) → score 8
Tampon     : 1000/2000 = 50% retiré (>40%) → score 6
Historique : 2 dépôts vs 1 retrait → dépôts > retraits → score 4
Objectifs  : chute de progression entre 5 et 15% → score 4

riskScore = 4×0.35 + 8×0.25 + 6×0.20 + 4×0.20 = 1.40+2.00+1.20+0.80 = 5.4 → 5 (MEDIUM)
```

**Même exemple avec 100 DT :**
```
Couverture : solde restant = 1900 DT → 1900/1510 = 1.26 mois (<2 mois) → score 6
Tampon     : 100/2000 = 5% retiré (≤20%) → score 2
riskScore = 2×0.35 + 6×0.25 + 2×0.20 + 4×0.20 = 0.70+1.50+0.40+0.80 = 3.4 → 3 (LOW)
```

**Le score change avec le montant** car deux facteurs dépendent directement du montant : la couverture des dépenses (solde restant) et le tampon (% retiré). L'historique ne change pas puisqu'il reflète le comportement passé.

```
riskScore = goalImpact×0.35 + expenseCoverage×0.25 + buffer×0.20 + history×0.20
```

**Niveaux :** LOW (<4) / MEDIUM (4–5) / HIGH (6–7) / CRITICAL (≥8)

---

## 10. Calculateur de Fonds d'Urgence
`GET /api/ai/savings/emergency-fund`

**Principe :** Analyse les dépenses réelles mois par mois sur 6 mois pour calculer le fonds d'urgence recommandé.

**Calcul :**
```
avgMonthlyExpenses = totalDépenses6Mois / 6  (base fixe, même si certains mois = 0)
minimum  = avgMonthlyExpenses × 3  (seuil minimum recommandé)
optimal  = avgMonthlyExpenses × 6  (seuil optimal)
coverage = currentSavings / avgMonthlyExpenses  (en mois)
```

**Niveaux de protection :**
```
NONE      → < 0.5 mois
LOW       → 0.5 – 1.5 mois
MODERATE  → 1.5 – 3 mois
GOOD      → 3 – 6 mois
EXCELLENT → > 6 mois
```

**Calcul contribution mensuelle nécessaire :**
```
pourAtteindreMinimum = (minimum - currentSavings) / 6
pourAtteindreOptimal = (optimal  - currentSavings) / 6
```

---

## 11. Dashboard Décisionnel (Cross-Module)
`GET /api/ai/dashboard`

**Principe :** Vue diagnostique unifiée qui croise comptabilité + épargne pour générer des insights et actions prioritaires personnalisées.

**Sources de données :**
- `accounting_entry` → revenus et dépenses réels des **3 derniers mois**
- `savings_account` → solde épargne actuel (`currentBalance`)
- Appel interne à `getFinancialHealthScore` (score 0–100)

**Calculs effectués :**
```
monthlyIncome    = totalRevenu3Mois / 3
monthlyExpenses  = totalDépense3Mois / 3
monthlySavings   = monthlyIncome - monthlyExpenses

emergencyCoverage = savingsBalance / monthlyExpenses  (en mois)

profitTrend : profit net M-2 vs M0 → GROWING / DECLINING / STABLE
```

**Ce qu'il affiche et pourquoi :**

| Champ | Pourquoi |
|---|---|
| `financialHealthScore` + `healthLevel` | Vue globale de l'état financier (appel interne au health score) |
| `monthlyIncome / monthlyExpenses / monthlySavings` | Résumé chiffré de la situation mensuelle réelle |
| `savingsBalance` | Solde épargne actuel |
| `emergencyFundCoverage` | Combien de mois de dépenses l'épargne peut couvrir |
| `profitabilityTrend` | Est-ce que la rentabilité monte ou descend sur 3 mois |
| `keyInsights` | Messages automatiques : score ≥ 70 → SUCCESS / épargne négative → ALERT / coverage < 3 mois → WARNING / tendance DECLINING → WARNING |
| `prioritizedActions` | Actions concrètes ordonnées par gravité : épargne négative → fonds urgence → ratio dépenses → diversification |

**C'est un endpoint de diagnostic** — il répond à "comment tu vas ?" en croisant toutes les données disponibles.

---

## 12. Optimiseur de Flux de Trésorerie
`GET /api/ai/dashboard/optimize-cashflow`

**Principe :** Vue prescriptive — applique des taux de réduction fixes par catégorie (normes agricoles) et calcule l'impact concret sur le flux net.

**Source de données :** `accounting_entry` → dépenses réelles des **3 derniers mois**, par catégorie.

**Taux de réduction par catégorie (normes agricoles fixes) :**
```
TRANSPORT  → -15%  (regrouper les livraisons)
OTHER      → -20%  (meilleure planification)
LABOR      → -10%  (optimisation planning)
EQUIPMENT  → -10%  (maintenance préventive)
FERTILIZER →  -5%  (analyse de sol)
SEEDS      →  -5%  (achats groupés)
INSURANCE, LOAN_PAYMENT → 0% (fixes, non modifiables)
```

**Calcul par catégorie :**
```
currentAmount   = totalRéel3Mois / 3
reduction       = currentAmount × reductionPct
suggestedAmount = currentAmount - reduction
```

**Suggestion ignorée** si économie < 5 DT (pas de bruit inutile).

**Calculs globaux :**
```
optimizedNet         = currentNet + somme(toutes les réductions)
idealSavings         = monthlyIncome × 20%
idealExpensesCeiling = monthlyIncome × 80%
```

**Ce qu'il affiche et pourquoi :**

| Champ | Pourquoi |
|---|---|
| `suggestions` | Liste des catégories optimisables : montant actuel → montant suggéré, économie possible, justification |
| `potentialImprovement` | Total des économies si toutes les suggestions appliquées |
| `currentMonthlyNet` vs `optimizedMonthlyNet` | Avant/après — montre l'impact concret en DT |
| `idealSavingsAllocation` | Ce que tu devrais mettre de côté (20% des revenus) |
| `idealExpensesCeiling` | Plafond de dépenses recommandé (80% des revenus) |
| `overallStrategy` | Message global : flux négatif → urgence / flux faible → optimiser / flux excellent → maintenir |

**Différence entre les deux endpoints :**

| `/dashboard` | `/optimize-cashflow` |
|---|---|
| Vue **diagnostique** — "comment tu vas ?" | Vue **prescriptive** — "que faire concrètement ?" |
| Score santé + tendance + insights + alertes | Suggestions chiffrées par catégorie |
| Croise comptabilité **et** épargne | Comptabilité uniquement |
