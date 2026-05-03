# AgriProtect — Scénario de Présentation

> **Base URL :** `http://localhost:8085`
> **Auth :** Bearer Token (copier depuis étape 2, coller dans Authorization → Bearer Token pour toutes les requêtes suivantes)

---

## 1. Créer un utilisateur

`POST /agri/users`
```json
{
  "email": "ahmed@agri.tn",
  "password": "password123",
  "firstName": "Ahmed",
  "lastName": "Ben Ali",
  "role": "FARMER",
  "phoneNumber": "12345678",
  "address": "Sfax, Tunisie"
}
```

---

## 2. Login → récupérer le token

`POST /agri/auth/login`
```json
{
  "email": "ahmed@agri.tn",
  "password": "password123"
}
```
> Copier le `token` → l'utiliser dans toutes les requêtes suivantes.

---

## 3. Créer le compte épargne

`POST /api/savings/accounts`
```json
{
  "accountName": "Epargne exploitation agricole",
  "monthlySavingsTarget": 500.00
}
```

---

## 4. Créer les objectifs d'épargne

`POST /api/savings/goals`
```json
{
  "goalName": "Achat nouveau tracteur",
  "targetAmount": 10000.00,
  "targetDate": "2027-06-01",
  "description": "Remplacer le tracteur vieillissant"
}
```

`POST /api/savings/goals`
```json
{
  "goalName": "Systeme irrigation goutte-a-goutte",
  "targetAmount": 3000.00,
  "targetDate": "2026-09-01",
  "description": "Moderniser l'irrigation pour economiser l'eau"
}
```

`POST /api/savings/goals`
```json
{
  "goalName": "Fonds urgence exploitation",
  "targetAmount": 5000.00,
  "targetDate": "2026-12-01",
  "description": "Reserve pour imprevus"
}
```

> Consulter tous les goals : `GET /api/savings/goals`

> Consulter un goal : `GET /api/savings/goals/{goalId}`

> Modifier un goal : `PUT /api/savings/goals/{goalId}`
```json
{
  "goalName": "Achat nouveau tracteur",
  "targetAmount": 12000.00,
  "targetDate": "2027-06-01",
  "description": "Budget augmente pour modele 2027"
}
```

---

## 5. Définir la priorité d'allocation

> Tracteur financé en priorité absolue avant tous les autres goals.

`PATCH /api/savings/goals/{tracteurGoalId}/priority`
```json
{ "priority": 1 }
```

> Irrigation reçoit toujours 30% fixe du solde total.

`PATCH /api/savings/goals/{irrigationGoalId}/priority`
```json
{ "customAllocationPercentage": 30.0 }
```

> Réinitialiser un goal en mode automatique proportionnel :

`PATCH /api/savings/goals/{fondsUrgenceGoalId}/priority`
```json
{ "resetToAuto": true }
```

> Vérifier les modes : `GET /api/savings/goals`

---

## 6. Alimenter le compte (dépôts)

`POST /api/savings/transactions`
```json
{ "type": "DEPOSIT", "amount": 1500.00, "description": "Epargne mensuelle" }
```

`POST /api/savings/transactions`
```json
{ "type": "DEPOSIT", "amount": 2000.00, "description": "Revenu recolte olives" }
```

`POST /api/savings/transactions`
```json
{ "type": "DEPOSIT", "amount": 1000.00, "description": "Vente cereales" }
```

> Vérifier la redistribution : `GET /api/savings/goals`
> → Tracteur (P1) financé en premier, irrigation ses 30%, fonds urgence le reste proportionnellement.

> Effectuer un retrait : `POST /api/savings/transactions`
```json
{ "type": "WITHDRAWAL", "amount": 300.00, "description": "Achat urgent semences" }
```

> Consulter toutes les transactions : `GET /api/savings/transactions`

---

## 7. Simuler un retrait (sans l'effectuer)

`POST /api/savings/accounts/simulate/withdraw`
```json
{ "amount": 1500.00 }
```

---

## 8. Archiver un goal atteint

> Quand `achieved = true` sur un goal (currentAmount >= targetAmount), le collecter pour l'archiver.

`POST /api/savings/goals/{goalId}/collect`

> Consulter les goals archivés : `GET /api/savings/goals/archive`

> Le solde est automatiquement redistribué vers les goals restants après archivage.

---

## 9. Saisir les écritures comptables

`POST /api/accounting/entries`
```json
{ "entryType": "INCOME", "category": "SALES", "amount": 3000.00, "description": "Vente 500kg olives", "entryDate": "2025-10-15" }
```
```json
{ "entryType": "INCOME", "category": "SALES", "amount": 2500.00, "description": "Vente legumes marche", "entryDate": "2025-11-20" }
```
```json
{ "entryType": "INCOME", "category": "SALES", "amount": 3200.00, "description": "Vente cereales", "entryDate": "2025-12-10" }
```
```json
{ "entryType": "INCOME", "category": "SALES", "amount": 2800.00, "description": "Vente fruits", "entryDate": "2026-01-18" }
```
```json
{ "entryType": "INCOME", "category": "SALES", "amount": 3500.00, "description": "Vente tomates", "entryDate": "2026-02-22" }
```
```json
{ "entryType": "EXPENSE", "category": "SEEDS", "amount": 450.00, "description": "Semences ble printemps", "entryDate": "2026-02-10" }
```
```json
{ "entryType": "EXPENSE", "category": "FERTILIZER", "amount": 680.00, "description": "Engrais printemps", "entryDate": "2026-02-05" }
```
```json
{ "entryType": "EXPENSE", "category": "LABOR", "amount": 750.00, "description": "Ouvriers taille oliviers", "entryDate": "2026-02-14" }
```
```json
{ "entryType": "EXPENSE", "category": "TRANSPORT", "amount": 250.00, "description": "Livraison marche gros", "entryDate": "2025-12-08" }
```
```json
{ "entryType": "EXPENSE", "category": "IRRIGATION", "amount": 350.00, "description": "Maintenance pompe eau", "entryDate": "2026-01-20" }
```
```json
{ "entryType": "EXPENSE", "category": "EQUIPMENT", "amount": 1200.00, "description": "Reparation tracteur", "entryDate": "2026-01-10" }
```

> Consulter toutes les écritures : `GET /api/accounting/entries`

> Filtrer par type/catégorie/période : `GET /api/accounting/entries?type=EXPENSE&category=FERTILIZER&from=2025-10-01&to=2026-02-28`

> Modifier une écriture : `PUT /api/accounting/entries/{id}`
```json
{ "entryType": "INCOME", "category": "SALES", "amount": 3500.00, "description": "Vente olives (corrige)", "entryDate": "2025-10-15" }
```

---

## 10. Créer des budgets

`POST /api/accounting/budgets`
```json
{ "periodType": "MONTHLY", "periodStart": "2026-02-01", "periodEnd": "2026-02-28", "category": "SEEDS", "plannedAmount": 500.00 }
```
```json
{ "periodType": "MONTHLY", "periodStart": "2026-02-01", "periodEnd": "2026-02-28", "category": "FERTILIZER", "plannedAmount": 700.00 }
```
```json
{ "periodType": "MONTHLY", "periodStart": "2026-02-01", "periodEnd": "2026-02-28", "category": "LABOR", "plannedAmount": 600.00 }
```

> Budgets du mois courant (avril) — volontairement bas pour déclencher les alertes prédictives :
```json
{ "periodType": "MONTHLY", "periodStart": "2026-04-01", "periodEnd": "2026-04-30", "category": "FERTILIZER", "plannedAmount": 200.00 }
```
```json
{ "periodType": "MONTHLY", "periodStart": "2026-04-01", "periodEnd": "2026-04-30", "category": "LABOR", "plannedAmount": 100.00 }
```

> Dépenses d'avril — nécessaires pour que le dailyBurnRate soit calculé et déclenche les alertes :
```json
{ "entryType": "EXPENSE", "category": "FERTILIZER", "amount": 180.00, "description": "Engrais avril", "entryDate": "2026-04-01" }
```
```json
{ "entryType": "EXPENSE", "category": "LABOR", "amount": 120.00, "description": "Ouvriers avril", "entryDate": "2026-04-01" }
```

> Consulter tous les budgets : `GET /api/accounting/budgets`

---

## 11. Analytics comptables

`GET /api/accounting/summary?from=2025-10-01&to=2026-02-28`
> Bilan : total revenus, dépenses, revenu net sur la période.

`GET /api/accounting/budget-vs-actual?periodStart=2026-02-01&periodEnd=2026-02-28`
> Budget prévu vs dépenses réelles par catégorie (OK / WARN / ALERT).

`GET /api/accounting/spending-breakdown?from=2025-10-01&to=2026-02-28`
> Décomposition des dépenses par catégorie avec pourcentage.

`GET /api/accounting/cashflow/forecast`
> Détecte les 3 derniers mois avec données réelles, calcule la tendance linéaire (variation mensuelle), et projette chaque mois futur depuis le dernier mois connu — chaque mois prédit est différent.

`GET /api/accounting/alerts/overspending?periodStart=2026-02-01&periodEnd=2026-02-28`
> Alertes dépassement de budget (MEDIUM / HIGH / CRITICAL).

`GET /api/accounting/anomalies?from=2025-10-01&to=2026-02-28`
> Détecte les transactions dont le montant dépasse 2x la moyenne de leur catégorie.

> Démo Anomalie 

`POST /api/accounting/entries`
```json
{ "entryType": "EXPENSE", "category": "FERTILIZER", "amount": 5000.00, "description": "Achat engrais suspect", "entryDate": "2026-02-25" }
```


---

## 12. Analytics épargne

`GET /api/savings/accounts/goal/progress`
> Progression globale vers l'objectif d'épargne (%, statut, montant restant).

`GET /api/savings/accounts/summary/monthly?months=6`
> Résumé mois par mois : dépôts, retraits, variation nette, solde.

`GET /api/savings/accounts/alerts`
> Alertes : solde bas, goal proche (>90%), inactivité depuis 1 mois.

`GET /api/savings/accounts/recommendation`
> Recommande un montant mensuel à épargner basé sur les revenus/dépenses réels. ( 20% des revenus disponibles )


`GET /api/savings/accounts/statement?from=2026-01-01&to=2026-04-30`
> Relevé de compte complet avec solde ouverture/clôture et liste des transactions.

---

## 13. IA Comptabilité

### Score de santé financière
> Score 0-100 basé sur 5 métriques pondérées (ratio dépenses, régularité, diversification, épargne, tendance).

`GET /api/ai/accounting/health-score`

---

### Prévision des dépenses
> Weighted Moving Average sur 6 mois — prédit les dépenses futures par catégorie avec tendance.

`GET /api/ai/accounting/forecast/expenses?months=3`

---

### Simulateur What-If
> Simule l'impact de modifier les dépenses/revenus par % — montre le revenu net projeté sur N mois.

`POST /api/ai/accounting/simulate/what-if`
```json
{
  "categoryChanges": {
    "FERTILIZER": -20,
    "LABOR": 10,
    "TRANSPORT": -30
  },
  "incomeChangePercent": 15,
  "simulationMonths": 6
}
```

---

### Tendance de rentabilité
> Régression linéaire sur N mois — calcule slope, R², meilleur/pire mois, tendance GROWING/DECLINING/STABLE.

`GET /api/ai/accounting/trends/profitability?months=6`

---

### Catégorisation intelligente (NLP)
> Analyse une description textuelle et suggère la catégorie avec un score de confiance.

`POST /api/ai/accounting/categorize`
```json
{ "description": "Achat de semences de ble pour la plantation de printemps" }
```
```json
{ "description": "Reparation du tracteur John Deere" }
```
```json
{ "description": "Salaire des ouvriers agricoles" }
```

---

### Alertes budget prédictives
> Calcule le daily burn rate et prédit si le budget sera dépassé avant la fin du mois.

`GET /api/ai/accounting/budget/predictive-alerts`

---

## 14. IA Épargne

### Prédiction d'atteinte des objectifs
> Moyenne pondérée des 7 derniers mois pour prédire quand chaque goal sera atteint (ON_TRACK / AT_RISK).

`GET /api/ai/savings/goal/predict-achievement`

---

### Plan d'épargne intelligent
> Génère un plan mensuel personnalisé tenant compte des patterns saisonniers de revenus/dépenses.

`GET /api/ai/savings/plan/smart`

---

### Score de risque d'un retrait
> Évalue le risque d'un retrait via 4 facteurs pondérés — score 1 (faible) à 10 (critique).

`POST /api/ai/savings/withdrawal/risk-assessment`
```json
{ "amount": 2000.00 }
```

---

### Calculateur fonds d'urgence
> Analyse les dépenses réelles des 6 derniers mois et calcule le niveau de protection (NONE → EXCELLENT).

`GET /api/ai/savings/emergency-fund`

---

## 15. Tableau de bord décisionnel (cross-module)

### Dashboard unifié
> Croise comptabilité + épargne : score santé, insights intelligents, actions prioritaires personnalisées.

`GET /api/ai/dashboard`

---

### Optimiseur de flux de trésorerie
> Propose des réductions par catégorie basées sur normes agricoles + calcule le plafond de dépenses idéal.

`GET /api/ai/dashboard/optimize-cashflow`

---

## Bonus — Démontrer le Smart Sync en direct

> Ajouter un nouveau goal après avoir déjà du solde → redistribution instantanée.

`POST /api/savings/goals`
```json
{
  "goalName": "Serre agricole",
  "targetAmount": 8000.00,
  "targetDate": "2027-03-01",
  "description": "Construction d'une serre pour cultures protegees"
}
```
> `GET /api/savings/goals` → le solde existant est redistribué vers le nouveau goal instantanément.

> Supprimer un goal : `DELETE /api/savings/goals/{goalId}` → redistribution automatique vers les goals restants.
