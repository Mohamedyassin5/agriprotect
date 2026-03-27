# AgriProtect - Postman Testing Guide (Beginner Friendly)

## What is Postman?

Postman is a free app that lets you **send HTTP requests** to your API and see the responses. Think of it like a browser, but for APIs instead of websites.

**Download:** https://www.postman.com/downloads/

---

## Legend: Types d'Endpoints

| Tag | Signification |
|-----|---------------|
| **CRUD** | Operation basique : Create, Read, Update, Delete |
| **BESOIN FONCTIONNEL** | Logique metier : analytics, alertes, recommandations, simulations, previsions |
| **METIER AVANCE — IA** | Intelligence Artificielle : algorithmes predictifs, NLP, regression lineaire, moyenne mobile ponderee |
| **METIER AVANCE — AIDE A LA DECISION** | Outil d'aide a la prise de decision : simulateurs, scores de risque, dashboards, calculateurs |

---

## Quick Setup (5 minutes)

### Step 1: Open Postman
After installing, open Postman and skip the sign-in (click "Skip and go to app" at the bottom).

### Step 2: Set your Base URL
Your API runs at: **`http://localhost:8085`**

> Make sure your Spring Boot app is running before testing!

### Step 3: How to make a request
1. Click the **"+"** tab at the top to create a new request
2. Choose the method (GET, POST, PUT, DELETE) from the dropdown
3. Type the full URL (e.g., `http://localhost:8085/agri/auth/login`)
4. For POST/PUT requests, go to the **Body** tab → select **raw** → choose **JSON** from the dropdown
5. Click **Send**

---

## Testing Order (Follow This!)

You MUST test in this order because some endpoints need data from previous ones.

---

## STEP 1: Create a User — `CRUD`

**Method:** `POST`
**URL:** `http://localhost:8085/agri/users`
**Body** (raw → JSON):
```json
{
  "email": "farmer@test.com",
  "password": "password123",
  "firstName": "Ahmed",
  "lastName": "Ben Ali",
  "role": "FARMER",
  "phoneNumber": "12345678",
  "address": "Tunis, Tunisia"
}
```

**Expected:** You get back the created user with an `id` field.

---

## STEP 2: Login (Get Your Token) — `CRUD`

**Method:** `POST`
**URL:** `http://localhost:8085/agri/auth/login`
**Body** (raw → JSON):
```json
{
  "email": "farmer@test.com",
  "password": "password123"
}
```

**Expected:** You get back something like:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWI..."
}
```

### IMPORTANT: Copy the token!
You need this token for ALL the next requests. Here's how to use it:

1. In each new request, go to the **Authorization** tab
2. In the **Type** dropdown, select **Bearer Token**
3. Paste your token in the **Token** field

> **Tip:** The token is valid for 24 hours. If you get a 401/403 error later, just login again to get a new token.

---

## STEP 3: Savings Account — `CRUD`

> **Smart Goal Sync** : Les champs `goalAmount` et `goalTitle` du compte sont **auto-calcules** depuis vos Savings Goals (Step 5). Vous n'avez plus besoin de les definir manuellement.
> - `goalAmount` = somme des `targetAmount` de tous vos goals actifs
> - `goalTitle` = resume automatique ("3 objectif(s): Tracteur, Irrigation, ...")
> - Chaque depot/retrait redistribue le solde proportionnellement a vos goals

### 3a. Create Savings Account — `CRUD`

**Method:** `POST`
**URL:** `http://localhost:8085/api/savings/accounts`
**Auth:** Bearer Token (paste your token)
**Body** (raw → JSON):
```json
{
  "accountName": "Epargne exploitation agricole",
  "monthlySavingsTarget": 500.00
}
```

**Ce que ca fait :** Cree un compte epargne. `goalAmount` et `goalTitle` seront auto-calcules depuis les Savings Goals (Step 5).

**Expected:**
```json
{
  "id": 1,
  "userId": "...",
  "accountName": "Epargne exploitation agricole",
  "currentBalance": 0.00,
  "monthlySavingsTarget": 500.00,
  "goalAmount": null,
  "goalTitle": null,
  "status": "ACTIVE",
  "createdAt": "...",
  "updatedAt": "..."
}
```

### 3b. View My Savings Account — `CRUD`

**Method:** `GET`
**URL:** `http://localhost:8085/api/savings/accounts/me`
**Auth:** Bearer Token
**Body:** none

**Apres avoir cree des goals et fait des depots :**
```json
{
  "id": 1,
  "userId": "...",
  "accountName": "Epargne exploitation agricole",
  "currentBalance": 3800.00,
  "monthlySavingsTarget": 500.00,
  "goalAmount": 18000.00,
  "goalTitle": "3 objectif(s): Achat nouveau tracteur, Fonds urgence exploitation, Systeme irrigation goutte",
  "status": "ACTIVE",
  "createdAt": "...",
  "updatedAt": "..."
}
```

### 3c. Update My Savings Account — `CRUD`

**Method:** `PUT`
**URL:** `http://localhost:8085/api/savings/accounts/me`
**Auth:** Bearer Token
**Body** (raw → JSON):
```json
{
  "accountName": "Fonds agricole principal",
  "monthlySavingsTarget": 800.00
}
```

> **Champs modifiables :**
> - `accountName` : renommer le compte
> - `monthlySavingsTarget` : changer l'objectif d'epargne mensuel
> - `status` : `"ACTIVE"` ou `"FROZEN"` (un compte FROZEN bloque les depots/retraits)
>
> **Champs auto-calcules (non modifiables) :** `goalAmount`, `goalTitle`, `currentBalance`

### 3d. Freeze My Account — `CRUD`

**Method:** `PUT`
**URL:** `http://localhost:8085/api/savings/accounts/me`
**Auth:** Bearer Token
**Body** (raw → JSON):
```json
{
  "status": "FROZEN"
}
```

> Un compte FROZEN bloque toute transaction (depot/retrait). Pour reactiver :
```json
{
  "status": "ACTIVE"
}
```

---

## STEP 4: Savings Transactions — `CRUD`

### 4a. Make a Deposit — `CRUD`

**Method:** `POST`
**URL:** `http://localhost:8085/api/savings/transactions`
**Auth:** Bearer Token
**Body** (raw → JSON):
```json
{
  "type": "DEPOSIT",
  "amount": 1500.00,
  "description": "Monthly farm savings"
}
```

### 4b. Make Another Deposit — `CRUD`

**Same URL and method as 4a.**
```json
{
  "type": "DEPOSIT",
  "amount": 2000.00,
  "description": "Olive harvest income saved"
}
```

### 4c. Make a Withdrawal — `CRUD`

**Same URL and method as 4a.**
```json
{
  "type": "WITHDRAWAL",
  "amount": 500.00,
  "description": "Emergency seed purchase"
}
```

### 4d. List All Transactions — `CRUD`

**Method:** `GET`
**URL:** `http://localhost:8085/api/savings/transactions`
**Auth:** Bearer Token

With filters (optional):
- `http://localhost:8085/api/savings/transactions?type=DEPOSIT`
- `http://localhost:8085/api/savings/transactions?from=2026-01-01T00:00:00&to=2026-12-31T23:59:59`

### 4e. Get One Transaction — `CRUD`

**Method:** `GET`
**URL:** `http://localhost:8085/api/savings/transactions/1`
**Auth:** Bearer Token

---

## STEP 5: Savings Goals — `CRUD` + `SMART SYNC`

> **Prerequis :** Avoir cree un compte epargne (Step 3).
>
> **Smart Goal Sync** : Les goals sont maintenant **automatiquement synchronises** avec votre compte epargne :
> - **Depot/Retrait** → le solde est redistribue proportionnellement a chaque goal selon son `targetAmount`
> - **Creer un goal** → `goalAmount` et `goalTitle` du compte sont mis a jour automatiquement
> - **Supprimer un goal** → redistribution vers les goals restants
> - **Goal atteint** → auto-marque `achieved = true` quand `currentAmount >= targetAmount`
> - **`currentAmount` n'est plus modifiable manuellement** — il est calcule automatiquement

### 5a. Create a Savings Goal — `CRUD`

**Method:** `POST`
**URL:** `http://localhost:8085/api/savings/goals`
**Auth:** Bearer Token
**Body** (raw → JSON):
```json
{
  "goalName": "Achat nouveau tracteur",
  "targetAmount": 10000.00,
  "targetDate": "2027-06-01",
  "description": "Remplacer le tracteur vieillissant"
}
```

> Note : `currentAmount` n'est plus dans le body — il est auto-calcule par le Smart Goal Sync.

**Expected :** Le goal est cree et `currentAmount` est automatiquement calcule a partir du solde actuel du compte.
```json
{
  "id": "...",
  "savingsAccountId": 1,
  "goalName": "Achat nouveau tracteur",
  "targetAmount": 10000.00,
  "currentAmount": 3800.00,
  "targetDate": "2027-06-01",
  "description": "Remplacer le tracteur vieillissant",
  "achieved": false,
  "progressPercentage": 38.0,
  "createdAt": "...",
  "updatedAt": "..."
}
```

**Verifiez le compte :** `GET /api/savings/accounts/me` → `goalAmount` et `goalTitle` sont maintenant remplis automatiquement.

### 5b. Create Another Goal — `CRUD`

**Same URL and method as 5a.**
```json
{
  "goalName": "Systeme irrigation goutte",
  "targetAmount": 3000.00,
  "targetDate": "2026-09-01",
  "description": "Moderniser l'irrigation pour economiser l'eau"
}
```

> Apres creation du 2e goal, le solde (3800 DT) est **redistribue** :
> - Tracteur (10000 target, 77%) → `currentAmount` = 2923.08
> - Irrigation (3000 target, 23%) → `currentAmount` = 876.92
>
> Verifiez : `GET /api/savings/accounts/me` → `goalAmount = 13000`, `goalTitle = "2 objectif(s): Achat nouveau tracteur, Systeme irrigation goutte"`

### 5c. List All My Goals — `CRUD`

**Method:** `GET`
**URL:** `http://localhost:8085/api/savings/goals`
**Auth:** Bearer Token

With filters (optional):
- Achieved goals only: `http://localhost:8085/api/savings/goals?achieved=true`
- Unachieved goals only: `http://localhost:8085/api/savings/goals?achieved=false`

### 5d. Get One Goal — `CRUD`

**Method:** `GET`
**URL:** `http://localhost:8085/api/savings/goals/{goalId}`
**Auth:** Bearer Token
(Replace `{goalId}` with the actual goal ID from Step 5a)

### 5e. Update a Goal — `CRUD`

**Method:** `PUT`
**URL:** `http://localhost:8085/api/savings/goals/{goalId}`
**Auth:** Bearer Token
**Body** (raw → JSON):
```json
{
  "goalName": "Tracteur neuf (modele 2027)",
  "targetAmount": 12000.00,
  "targetDate": "2027-06-01",
  "description": "Budget augmente pour un modele plus recent"
}
```

> Note : `currentAmount` n'est plus dans le body d'update — il sera recalcule automatiquement apres le changement de `targetAmount`.

### 5f. Delete a Goal — `CRUD`

**Method:** `DELETE`
**URL:** `http://localhost:8085/api/savings/goals/{goalId}`
**Auth:** Bearer Token

> Apres suppression, le solde est redistribue aux goals restants et `goalAmount`/`goalTitle` du compte sont mis a jour.

### 5g. Collect an Achieved Goal (Archive) — `METIER AVANCE`

**Method:** `POST`
**URL:** `http://localhost:8085/api/savings/goals/{goalId}/collect`
**Auth:** Bearer Token
**Body:** none

> Un goal atteint (`achieved = true`) peut etre "collecte" : il disparait de la liste active et va dans l'archive. Le solde est redistribue aux goals restants.

**Expected:** Le goal avec `collected = true` et `collectedAt` rempli.

### 5h. View Archived Goals — `CRUD`

**Method:** `GET`
**URL:** `http://localhost:8085/api/savings/goals/archive`
**Auth:** Bearer Token
**Body:** none

> Affiche tous les goals collectes (archives). Ces goals ne participent plus a la distribution du solde.

### 5i. Definir la Priorite d'un Goal — `METIER AVANCE`

**Method:** `PATCH`
**URL:** `http://localhost:8085/api/savings/goals/{goalId}/priority`
**Auth:** Bearer Token
**Body** (raw → JSON):

**Cas 1 — Priorite haute (financer ce goal en premier) :**
```json
{
  "priority": 1
}
```
> Le solde residuel (apres les goals a pourcentage fixe) va d'abord financer ce goal a 100%, avant les autres.

**Cas 2 — Pourcentage fixe (toujours allouer 40% du solde a ce goal) :**
```json
{
  "customAllocationPercentage": 40.0
}
```
> Ce goal recoit exactement 40% du solde total du compte, peu importe les autres goals.

**Cas 3 — Priorite + pourcentage (les deux a la fois) :**
```json
{
  "priority": 2,
  "customAllocationPercentage": 30.0
}
```

**Cas 4 — Reinitialiser a automatique :**
```json
{
  "resetToAuto": true
}
```
> Remet le goal en mode proportionnel automatique (priority = 0, customAllocationPercentage = null).

**Modes d'allocation resultants :**
- `AUTO_PROPORTIONAL` — priority=0, pas de % fixe → distribue proportionnellement au targetAmount
- `AUTO_PRIORITY` — priority > 0, pas de % fixe → finance ce goal en premier (P1 avant P2, etc.)
- `CUSTOM_PERCENTAGE` — customAllocationPercentage defini → recoit exactement ce % du solde

**Expected :** Le goal mis a jour avec le nouveau `allocationMode`, `priority`, `customAllocationPercentage`, et `remainingAmount`.
```json
{
  "id": "...",
  "goalName": "Achat nouveau tracteur",
  "targetAmount": 10000.00,
  "currentAmount": 3800.00,
  "remainingAmount": 6200.00,
  "priority": 1,
  "customAllocationPercentage": null,
  "allocationMode": "AUTO_PRIORITY",
  "progressPercentage": 38.0,
  "achieved": false
}
```

> **Validation :** La somme de tous les `customAllocationPercentage` ne peut pas depasser 100%. Une erreur 400 est retournee sinon.

### 5j. Tester le Smart Sync — `METIER AVANCE`

**Pour voir le sync en action :**

1. Faites un depot (Step 4a) : `POST /api/savings/transactions`
```json
{ "type": "DEPOSIT", "amount": 1000.00, "description": "Test sync" }
```

2. Verifiez les goals (Step 5c) : `GET /api/savings/goals`
→ `currentAmount` a augmente selon le mode d'allocation de chaque goal :
  - Goals `CUSTOM_PERCENTAGE` : ont recu exactement leur % du solde total
  - Goals `AUTO_PRIORITY` : ont ete finances dans l'ordre (P1 d'abord, puis P2...)
  - Goals `AUTO_PROPORTIONAL` : ont recu le reste proportionnellement

3. Verifiez le compte (Step 3b) : `GET /api/savings/accounts/me`
→ `currentBalance` a augmente de 1000, `goalTitle` inclut maintenant les modes : `"Tracteur [P1], Irrigation [P2], Semences [40%]"`

---

## STEP 6: Accounting Entries (Income & Expenses) — `CRUD`

### 6a. Add an Income Entry — `CRUD`

**Method:** `POST`
**URL:** `http://localhost:8085/api/accounting/entries`
**Auth:** Bearer Token
**Body** (raw → JSON):
```json
{
  "entryType": "INCOME",
  "category": "SALES",
  "amount": 3000.00,
  "description": "Sold 500kg of olives",
  "entryDate": "2026-02-15"
}
```

### 6b. Add an Expense Entry — `CRUD`

**Same URL and method as 5a.**
```json
{
  "entryType": "EXPENSE",
  "category": "SEEDS",
  "amount": 450.00,
  "description": "Wheat seeds for spring planting",
  "entryDate": "2026-02-10"
}
```

### 6c. Add More Expenses (for analytics to work better) — `CRUD`

**Same URL and method as 6a for each.**

**Fertilizer expense:**
```json
{
  "entryType": "EXPENSE",
  "category": "FERTILIZER",
  "amount": 800.00,
  "description": "Organic fertilizer",
  "entryDate": "2026-02-08"
}
```

**Labor expense:**
```json
{
  "entryType": "EXPENSE",
  "category": "LABOR",
  "amount": 600.00,
  "description": "Seasonal workers for harvest",
  "entryDate": "2026-02-12"
}
```

**Irrigation expense:**
```json
{
  "entryType": "EXPENSE",
  "category": "IRRIGATION",
  "amount": 350.00,
  "description": "Water pump maintenance",
  "entryDate": "2026-01-20"
}
```

**Another income:**
```json
{
  "entryType": "INCOME",
  "category": "SALES",
  "amount": 1200.00,
  "description": "Sold vegetables at market",
  "entryDate": "2026-01-25"
}
```

### Available Categories
Use these exact values for `category`:
- `SALES` - Selling produce
- `SEEDS` - Buying seeds
- `FERTILIZER` - Fertilizers
- `IRRIGATION` - Water/irrigation costs
- `LABOR` - Worker wages
- `TRANSPORT` - Transport costs
- `EQUIPMENT` - Tools/machines
- `INSURANCE` - Insurance payments
- `LOAN_PAYMENT` - Loan repayments
- `OTHER` - Anything else

### 6d. List All Entries — `CRUD`

**Method:** `GET`
**URL:** `http://localhost:8085/api/accounting/entries`
**Auth:** Bearer Token

With filters:
- By type: `?type=EXPENSE`
- By category: `?category=SEEDS`
- By date range: `?from=2026-01-01&to=2026-02-28`
- Combined: `?type=EXPENSE&category=LABOR&from=2026-01-01&to=2026-12-31`

### 6e. Get One Entry — `CRUD`

**Method:** `GET`
**URL:** `http://localhost:8085/api/accounting/entries/1`
**Auth:** Bearer Token

### 6f. Update an Entry — `CRUD`

**Method:** `PUT`
**URL:** `http://localhost:8085/api/accounting/entries/1`
**Auth:** Bearer Token
**Body** (raw → JSON):
```json
{
  "entryType": "INCOME",
  "category": "SALES",
  "amount": 3500.00,
  "description": "Sold 500kg of olives (price updated)",
  "entryDate": "2026-02-15"
}
```

### 6g. Delete an Entry — `CRUD`

**Method:** `DELETE`
**URL:** `http://localhost:8085/api/accounting/entries/3`
**Auth:** Bearer Token

---

## STEP 7: Budgets — `CRUD`

### 7a. Create a Monthly Budget — `CRUD`

**Method:** `POST`
**URL:** `http://localhost:8085/api/accounting/budgets`
**Auth:** Bearer Token
**Body** (raw → JSON):
```json
{
  "periodType": "MONTHLY",
  "periodStart": "2026-02-01",
  "periodEnd": "2026-02-28",
  "category": "SEEDS",
  "plannedAmount": 500.00
}
```

### 7b. Create More Budgets — `CRUD`

**Same URL and method as 7a.**

**Fertilizer budget:**
```json
{
  "periodType": "MONTHLY",
  "periodStart": "2026-02-01",
  "periodEnd": "2026-02-28",
  "category": "FERTILIZER",
  "plannedAmount": 700.00
}
```

**Labor budget:**
```json
{
  "periodType": "MONTHLY",
  "periodStart": "2026-02-01",
  "periodEnd": "2026-02-28",
  "category": "LABOR",
  "plannedAmount": 1000.00
}
```

### Available Period Types
- `MONTHLY`
- `SEASONAL`
- `YEARLY`

### 7c. List All Budgets — `CRUD`

**Method:** `GET`
**URL:** `http://localhost:8085/api/accounting/budgets`
**Auth:** Bearer Token

With filters:
- `?periodType=MONTHLY`
- `?category=SEEDS`

### 7d. Update a Budget — `CRUD`

**Method:** `PUT`
**URL:** `http://localhost:8085/api/accounting/budgets/1`
**Auth:** Bearer Token
**Body** (raw → JSON):
```json
{
  "periodType": "MONTHLY",
  "periodStart": "2026-02-01",
  "periodEnd": "2026-02-28",
  "category": "SEEDS",
  "plannedAmount": 600.00
}
```

### 7e. Delete a Budget — `CRUD`

**Method:** `DELETE`
**URL:** `http://localhost:8085/api/accounting/budgets/2`
**Auth:** Bearer Token

---

## STEP 8: Accounting Analytics — `BESOIN FONCTIONNEL`

> **Prerequis :** Vous devez avoir cree des entries (Step 6) et des budgets (Step 7) avant de tester ces endpoints.

### 8a. Financial Summary — `BESOIN FONCTIONNEL`

**Method:** `GET`
**URL:** `http://localhost:8085/api/accounting/summary?from=2026-01-01&to=2026-02-28`
**Auth:** Bearer Token

**Ce que ca fait :** Calcule le bilan financier sur une periode donnee : total des revenus, total des depenses, revenu net, et nombre de transactions par type.

**Resultat attendu :**
```json
{
  "periodStart": "2026-01-01",
  "periodEnd": "2026-02-28",
  "totalIncome": 4200.00,
  "totalExpenses": 2200.00,
  "netIncome": 2000.00,
  "incomeTransactionCount": 2,
  "expenseTransactionCount": 4
}
```

### 8b. Budget vs Actual — `BESOIN FONCTIONNEL`

**Method:** `GET`
**URL:** `http://localhost:8085/api/accounting/budget-vs-actual?periodStart=2026-02-01&periodEnd=2026-02-28`
**Auth:** Bearer Token

**Ce que ca fait :** Compare le budget prevu avec les depenses reelles pour chaque categorie. Indique si vous etes en dessous du budget (OK), proche de la limite (WARN), ou en depassement (ALERT).

**Resultat attendu :**
```json
{
  "periodStart": "2026-02-01",
  "periodEnd": "2026-02-28",
  "comparisons": [
    {
      "category": "SEEDS",
      "budgetedAmount": 500.00,
      "actualAmount": 450.00,
      "variance": 50.00,
      "percentageUsed": 90.0,
      "status": "WARN"
    },
    {
      "category": "FERTILIZER",
      "budgetedAmount": 700.00,
      "actualAmount": 800.00,
      "variance": -100.00,
      "percentageUsed": 114.29,
      "status": "ALERT"
    }
  ],
  "totalBudgeted": 2200.00,
  "totalActual": 1850.00,
  "totalVariance": 350.00
}
```

### 8c. Spending Breakdown — `BESOIN FONCTIONNEL`

**Method:** `GET`
**URL:** `http://localhost:8085/api/accounting/spending-breakdown?from=2026-01-01&to=2026-02-28`
**Auth:** Bearer Token

**Ce que ca fait :** Decompose les depenses par categorie avec le montant, le pourcentage de chaque categorie par rapport au total, et le nombre de transactions.

**Resultat attendu :**
```json
{
  "periodStart": "2026-01-01",
  "periodEnd": "2026-02-28",
  "totalExpenses": 2200.00,
  "breakdown": [
    { "category": "FERTILIZER", "amount": 800.00, "percentage": 36.36, "transactionCount": 1 },
    { "category": "LABOR", "amount": 600.00, "percentage": 27.27, "transactionCount": 1 },
    { "category": "SEEDS", "amount": 450.00, "percentage": 20.45, "transactionCount": 1 },
    { "category": "IRRIGATION", "amount": 350.00, "percentage": 15.91, "transactionCount": 1 }
  ]
}
```

### 8d. Cashflow Forecast — `BESOIN FONCTIONNEL`

**Method:** `GET`
**URL:** `http://localhost:8085/api/accounting/cashflow/forecast`
**Auth:** Bearer Token

**Ce que ca fait :** Prevoit les revenus et depenses pour les 3 prochains mois en se basant sur la moyenne des 3 derniers mois. Calcule un cumul pour voir l'evolution de la tresorerie.

**Resultat attendu :**
```json
{
  "currentBalance": 2000.00,
  "averageMonthlyIncome": 1400.00,
  "averageMonthlyExpenses": 733.33,
  "forecasts": [
    {
      "month": "MARCH 2026",
      "projectedIncome": 1400.00,
      "projectedExpenses": 733.33,
      "projectedNetCashflow": 666.67,
      "cumulativeCashflow": 666.67
    },
    {
      "month": "APRIL 2026",
      "projectedIncome": 1400.00,
      "projectedExpenses": 733.33,
      "projectedNetCashflow": 666.67,
      "cumulativeCashflow": 1333.34
    }
  ]
}
```

### 8e. Overspending Alerts — `BESOIN FONCTIONNEL`

**Method:** `GET`
**URL:** `http://localhost:8085/api/accounting/alerts/overspending?periodStart=2026-02-01&periodEnd=2026-02-28`
**Auth:** Bearer Token

**Ce que ca fait :** Detecte les categories ou les depenses reelles depassent le budget prevu. Attribue un niveau de severite : MEDIUM (< 20% de depassement), HIGH (20-50%), CRITICAL (> 50%).

**Condition pour avoir des resultats :** Il faut qu'une depense depasse son budget. Par exemple, si le budget FERTILIZER est 700 et que vous avez depense 800, l'alerte apparaitra.

**Resultat attendu (si depassement) :**
```json
{
  "alerts": [
    {
      "category": "FERTILIZER",
      "budgetedAmount": 700.00,
      "actualAmount": 800.00,
      "overspending": 100.00,
      "percentageOver": 14.29,
      "severity": "MEDIUM"
    }
  ],
  "totalAlertsCount": 1
}
```

> **Si le tableau `alerts` est vide :** Aucune categorie n'a depasse son budget. C'est normal si vos depenses sont inferieures aux budgets.
>
> **Pour declencher une alerte :** Ajoutez une depense qui depasse un budget. Exemple avec OTHER (budget = 100, depense actuelle = 80) :
> ```
> POST /api/accounting/entries
> ```
> ```json
> {
>   "entryType": "EXPENSE",
>   "category": "OTHER",
>   "amount": 150.00,
>   "description": "Achat imprévu materiel",
>   "entryDate": "2026-02-17"
> }
> ```
> Puis relancez le GET overspending → alerte OTHER avec depassement de 130%.

### 8f. Anomaly Detection — `BESOIN FONCTIONNEL`

**Method:** `GET`
**URL:** `http://localhost:8085/api/accounting/anomalies?from=2026-01-01&to=2026-02-28`
**Auth:** Bearer Token

**Ce que ca fait :** Detecte les transactions anormales dont le montant depasse 2x la moyenne de leur categorie. Utile pour reperer des erreurs de saisie ou des depenses inhabituelles.

**Condition pour avoir des resultats :** Il faut avoir plusieurs depenses dans une meme categorie, dont une qui est bien plus elevee que les autres. Par exemple :
- Depense FERTILIZER 200
- Depense FERTILIZER 150
- Depense FERTILIZER 5000 (celle-ci sera detectee comme anomalie)

Pour tester, ajoutez ces entries (POST `/api/accounting/entries`) :
```json
{"entryType": "EXPENSE", "category": "FERTILIZER", "amount": 200.00, "description": "Small fertilizer", "entryDate": "2026-02-05"}
```
```json
{"entryType": "EXPENSE", "category": "FERTILIZER", "amount": 150.00, "description": "Another small fertilizer", "entryDate": "2026-02-03"}
```
```json
{"entryType": "EXPENSE", "category": "FERTILIZER", "amount": 5000.00, "description": "Suspiciously expensive fertilizer", "entryDate": "2026-02-14"}
```

**Resultat attendu (apres ajout des entries ci-dessus) :**
```json
{
  "anomalies": [
    {
      "entryId": 9,
      "entryDate": "2026-02-14",
      "category": "FERTILIZER",
      "amount": 5000.00,
      "averageAmount": 1537.50,
      "deviationPercentage": 225.20,
      "reason": "Amount exceeds 2x category average"
    }
  ],
  "totalAnomaliesCount": 1
}
```

> **Si le tableau `anomalies` est vide :** Aucune transaction ne depasse 2x la moyenne de sa categorie. Ajoutez les entries de test ci-dessus pour declencher une anomalie.

---

## STEP 9: Savings Analytics — `BESOIN FONCTIONNEL`

> **Prerequis :** Vous devez avoir cree un compte epargne (Step 3) et effectue des transactions (Step 4).

### 9a. Goal Progress — `BESOIN FONCTIONNEL`

**Method:** `GET`
**URL:** `http://localhost:8085/api/savings/accounts/goal/progress`
**Auth:** Bearer Token

**Ce que ca fait :** Calcule votre progression vers votre objectif d'epargne. Indique le montant restant, le pourcentage atteint, et un statut ("Just Started" < 25%, "In Progress" 25-75%, "Near Goal" 75-99%, "Completed" >= 100%).

**Resultat attendu :**
```json
{
  "accountId": 1,
  "goalTitle": "Bigger Tractor Fund",
  "goalAmount": 8000.00,
  "currentBalance": 3000.00,
  "remaining": 5000.00,
  "progressPercentage": 37.50,
  "status": "In Progress"
}
```

### 9b. Monthly Savings Summary — `BESOIN FONCTIONNEL`

**Method:** `GET`
**URL:** `http://localhost:8085/api/savings/accounts/summary/monthly?months=6`
**Auth:** Bearer Token

**Ce que ca fait :** Resume l'activite d'epargne mois par mois sur les X derniers mois. Montre les depots, retraits, variation nette et solde pour chaque mois.

**Resultat attendu :**
```json
{
  "period": "2025-08-17 to 2026-02-17",
  "totalDeposits": 3500.00,
  "totalWithdrawals": 500.00,
  "netChange": 3000.00,
  "startBalance": 0.00,
  "endBalance": 3000.00,
  "transactionCount": 3,
  "monthlyData": [
    { "month": "AUGUST 2025", "deposits": 0, "withdrawals": 0, "netChange": 0, "balance": 0.00 },
    { "month": "SEPTEMBER 2025", "deposits": 0, "withdrawals": 0, "netChange": 0, "balance": 0.00 },
    { "month": "FEBRUARY 2026", "deposits": 3500.00, "withdrawals": 500.00, "netChange": 3000.00, "balance": 3000.00 }
  ]
}
```

### 9c. Savings Alerts — `BESOIN FONCTIONNEL`

**Method:** `GET`
**URL:** `http://localhost:8085/api/savings/accounts/alerts`
**Auth:** Bearer Token

**Ce que ca fait :** Detecte des situations qui meritent votre attention :
- **LOW_BALANCE** (WARNING) : Solde inferieur a 100
- **GOAL_NEAR** (INFO) : Progression >= 90% vers l'objectif
- **INACTIVITY** (INFO) : Aucune transaction depuis 1 mois

**Condition pour avoir des resultats :** Par defaut, si votre solde est > 100, votre objectif n'est pas proche, et vous avez des transactions recentes, le tableau sera vide (tout va bien !).

Pour tester l'alerte LOW_BALANCE, faites un gros retrait :
```
POST http://localhost:8085/api/savings/transactions
```
```json
{
  "type": "WITHDRAWAL",
  "amount": 2950.00,
  "description": "Big withdrawal to test alerts"
}
```

Puis retestez les alertes. Vous verrez :
```json
{
  "alerts": [
    {
      "type": "LOW_BALANCE",
      "severity": "WARNING",
      "message": "Your savings balance is low",
      "value": 50.00
    }
  ],
  "totalAlerts": 1
}
```

> **N'oubliez pas de re-deposer apres le test :**
> ```json
> {"type": "DEPOSIT", "amount": 2950.00, "description": "Refund after testing alerts"}
> ```

### 9d. Simulate Withdrawal — `BESOIN FONCTIONNEL`

**Method:** `POST`
**URL:** `http://localhost:8085/api/savings/accounts/simulate/withdraw`
**Auth:** Bearer Token
**Body** (raw → JSON):
```json
{
  "amount": 1000.00
}
```

**Ce que ca fait :** Simule un retrait SANS l'effectuer reellement. Montre le solde apres retrait, si c'est autorise, et l'impact sur votre objectif d'epargne. Utile pour prendre une decision avant de retirer.

**Resultat attendu :**
```json
{
  "currentBalance": 3000.00,
  "withdrawalAmount": 1000.00,
  "balanceAfterWithdrawal": 2000.00,
  "isAllowed": true,
  "message": "Withdrawal is allowed",
  "goalAmount": 8000.00,
  "goalImpact": 6000.00
}
```

**Testez aussi avec un montant trop eleve :**
```json
{
  "amount": 50000.00
}
```
Vous verrez `"isAllowed": false` et `"message": "Insufficient funds for this withdrawal"`.

### 9e. Savings Recommendation — `BESOIN FONCTIONNEL`

**Method:** `GET`
**URL:** `http://localhost:8085/api/savings/accounts/recommendation`
**Auth:** Bearer Token

**Ce que ca fait :** Analyse vos revenus et depenses des 3 derniers mois pour vous donner des conseils d'epargne personnalises. Recommande un montant mensuel a epargner (20% du revenu disponible) et detecte si vos depenses sont trop elevees.

**Resultat attendu :**
```json
{
  "currentSavingsBalance": 3000.00,
  "monthlyIncome": 1400.00,
  "monthlyExpenses": 733.33,
  "recommendedMonthlySavings": 133.33,
  "recommendations": [
    {
      "category": "SAVINGS_RATE",
      "recommendation": "Save at least 20% of your disposable income",
      "priority": "HIGH",
      "potentialSavings": 133.33
    }
  ]
}
```

### 9f. Account Statement — `BESOIN FONCTIONNEL`

**Method:** `GET`
**URL:** `http://localhost:8085/api/savings/accounts/statement?from=2026-01-01&to=2026-02-28`
**Auth:** Bearer Token

**Ce que ca fait :** Genere un releve de compte complet pour une periode donnee : solde d'ouverture, solde de cloture, total des depots et retraits, et la liste detaillee de toutes les transactions.

**Resultat attendu :**
```json
{
  "accountId": 1,
  "periodStart": "2026-01-01",
  "periodEnd": "2026-02-28",
  "openingBalance": 0.00,
  "closingBalance": 3000.00,
  "totalDeposits": 3500.00,
  "totalWithdrawals": 500.00,
  "transactions": [
    {
      "id": 1,
      "accountId": 1,
      "type": "DEPOSIT",
      "amount": 1500.00,
      "description": "Monthly farm savings",
      "occurredAt": "2026-02-17T10:30:00"
    }
  ]
}
```

---

## STEP 10: AI Comptabilite (Accounting AI) — `METIER AVANCE`

> **Prerequis :** Avoir cree des entries sur plusieurs mois (Step 6) et des budgets (Step 7). Plus vous avez de donnees, meilleurs seront les resultats IA.
>
> **Donnees de test recommandees :** Pour que l'IA fonctionne bien, ajoutez des entries sur les 6 derniers mois (changez les dates). Exemple rapide :

```json
{"entryType": "INCOME", "category": "SALES", "amount": 2500.00, "description": "Vente olives", "entryDate": "2025-09-15"}
{"entryType": "EXPENSE", "category": "SEEDS", "amount": 300.00, "description": "Semences ble", "entryDate": "2025-09-10"}
{"entryType": "INCOME", "category": "SALES", "amount": 3200.00, "description": "Vente legumes", "entryDate": "2025-10-20"}
{"entryType": "EXPENSE", "category": "FERTILIZER", "amount": 500.00, "description": "Engrais", "entryDate": "2025-10-05"}
{"entryType": "EXPENSE", "category": "LABOR", "amount": 700.00, "description": "Ouvriers", "entryDate": "2025-11-12"}
{"entryType": "INCOME", "category": "SALES", "amount": 1800.00, "description": "Vente cereales", "entryDate": "2025-11-25"}
{"entryType": "EXPENSE", "category": "TRANSPORT", "amount": 250.00, "description": "Livraison marche", "entryDate": "2025-12-08"}
{"entryType": "INCOME", "category": "SALES", "amount": 2800.00, "description": "Vente fruits", "entryDate": "2025-12-20"}
{"entryType": "EXPENSE", "category": "EQUIPMENT", "amount": 1200.00, "description": "Reparation tracteur", "entryDate": "2026-01-10"}
{"entryType": "EXPENSE", "category": "IRRIGATION", "amount": 400.00, "description": "Pompe eau", "entryDate": "2026-01-15"}
```

### 10a. Score de Sante Financiere — `METIER AVANCE — IA`

**Method:** `GET`
**URL:** `http://localhost:8085/api/ai/accounting/health-score`
**Auth:** Bearer Token

**Ce que ca fait :** Algorithme IA qui calcule un score global de sante financiere (0-100) en analysant 5 metriques ponderees :
- **Ratio Depenses/Revenus** (30%) — Part des revenus consommee
- **Regularite des Revenus** (20%) — Presence de revenus chaque mois
- **Diversification** (15%) — Nombre de sources de revenus
- **Taux d'Epargne** (20%) — Pourcentage de revenus non depenses
- **Tendance** (15%) — Evolution de la rentabilite sur 6 mois

Genere aussi des recommandations personnalisees basees sur les points faibles.

**Resultat attendu :**
```json
{
  "overallScore": 72,
  "healthLevel": "GOOD",
  "expenseRatio": {"name": "Ratio Depenses/Revenus", "score": 80, "weight": 0.3, "description": "..."},
  "incomeRegularity": {"name": "Regularite des Revenus", "score": 66, "weight": 0.2, "description": "..."},
  "diversification": {"name": "Diversification", "score": 25, "weight": 0.15, "description": "..."},
  "savingsRate": {"name": "Taux d Epargne", "score": 80, "weight": 0.2, "description": "..."},
  "trendScore": {"name": "Tendance", "score": 85, "weight": 0.15, "description": "..."},
  "recommendations": ["Elargissez vos categories de revenus pour reduire le risque."]
}
```

### 10b. Prevision des Depenses par IA — `METIER AVANCE — IA`

**Method:** `GET`
**URL:** `http://localhost:8085/api/ai/accounting/forecast/expenses?months=3`
**Auth:** Bearer Token

**Ce que ca fait :** Utilise un algorithme de moyenne mobile ponderee (Weighted Moving Average) sur 6 mois pour predire les depenses futures par categorie. Les mois recents ont plus de poids. Detecte aussi la tendance (INCREASING, DECREASING, STABLE) par categorie.

**Parametre optionnel :** `months` = nombre de mois a predire (defaut: 3)

**Resultat attendu :**
```json
{
  "forecastMonths": 3,
  "totalForecastedExpenses": 4500.00,
  "monthlyForecasts": [
    {"month": "MARCH 2026", "projectedExpenses": 1500.00, "confidence": 85, "trend": "HIGH_CONFIDENCE"}
  ],
  "categoryForecasts": [
    {"category": "FERTILIZER", "currentMonthlyAvg": 500.00, "forecastedAmount": 520.00, "growthRate": 4.0, "trend": "STABLE"}
  ],
  "methodology": "Weighted Moving Average (6 months) with seasonal trend detection"
}
```

### 10c. Simulateur What-If Budget — `METIER AVANCE — AIDE A LA DECISION`

**Method:** `POST`
**URL:** `http://localhost:8085/api/ai/accounting/simulate/what-if`
**Auth:** Bearer Token
**Body** (raw → JSON):
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

**Ce que ca fait :** Simulateur de scenarios "Et si...". Vous pouvez modifier les depenses par categorie (en %) et le revenu global, puis voir l'impact projete sur votre tresorerie. Exemple : "Que se passe-t-il si je reduis les engrais de 20% et augmente mon revenu de 15% ?"

**Donnees source :** Toutes les moyennes sont calculees depuis vos vraies ecritures comptables (`accounting_entry`) des 3 derniers mois. La reponse vous montre exactement combien d'ecritures ont ete utilisees et la formule appliquee pour chaque categorie.

**Resultat attendu :**
```json
{
  "analysisPeriodFrom": "2025-12-24",
  "analysisPeriodTo": "2026-03-24",
  "totalIncomeEntriesAnalyzed": 8,
  "totalExpenseEntriesAnalyzed": 10,
  "dataSource": "Base sur 8 ecritures REVENUS et 10 ecritures DEPENSES reelles entre le 2025-12-24 et le 2026-03-24 (moyenne sur 3 mois)",
  "currentMonthlyIncome": 2100.00,
  "currentMonthlyExpenses": 1400.00,
  "currentNetIncome": 700.00,
  "simulatedMonthlyIncome": 2415.00,
  "simulatedMonthlyExpenses": 1260.00,
  "simulatedNetIncome": 1155.00,
  "netImpact": 455.00,
  "verdict": "POSITIVE",
  "categoryImpacts": {
    "FERTILIZER": {
      "realTotalLast3Months": 1500.00,
      "numberOfEntries": 3,
      "currentAmount": 500.00,
      "simulatedAmount": 400.00,
      "changePercent": -20.0,
      "formula": "500.00 x (1 - 20%) = 400.00 TND/mois (total reel 3 ecritures: 1500.00 TND / 3 mois)"
    },
    "LABOR": {
      "realTotalLast3Months": 1800.00,
      "numberOfEntries": 3,
      "currentAmount": 600.00,
      "simulatedAmount": 660.00,
      "changePercent": 10.0,
      "formula": "600.00 x (1 + 10%) = 660.00 TND/mois (total reel 3 ecritures: 1800.00 TND / 3 mois)"
    },
    "TRANSPORT": {
      "realTotalLast3Months": 750.00,
      "numberOfEntries": 2,
      "currentAmount": 250.00,
      "simulatedAmount": 175.00,
      "changePercent": -30.0,
      "formula": "250.00 x (1 - 30%) = 175.00 TND/mois (total reel 2 ecritures: 750.00 TND / 3 mois)"
    }
  },
  "monthlyProjections": [
    {"month": "APRIL 2026", "projectedIncome": 2415.00, "projectedExpenses": 1260.00, "projectedNet": 1155.00, "cumulativeSavings": 1155.00},
    {"month": "MAY 2026", "projectedIncome": 2415.00, "projectedExpenses": 1260.00, "projectedNet": 1155.00, "cumulativeSavings": 2310.00}
  ]
}
```

**Comment lire la reponse :**
- `dataSource` → periode exacte et nombre d'ecritures reelles utilisees
- `realTotalLast3Months` → somme brute de vos ecritures sur 3 mois (verifiable dans Step 6)
- `numberOfEntries` → combien d'ecritures ont genere cette moyenne
- `currentAmount` → `realTotal / 3` = moyenne mensuelle reelle
- `formula` → calcul exact applique (ex : `500 × (1 - 20%) = 400`)
- `verdict` : `POSITIVE` (economie nette), `NEGATIVE` (perte nette), `NEUTRAL` (inchange)

### 10d. Analyse de Tendance de Rentabilite — `METIER AVANCE — IA`

**Method:** `GET`
**URL:** `http://localhost:8085/api/ai/accounting/trends/profitability?months=6`
**Auth:** Bearer Token

**Ce que ca fait :** Analyse la rentabilite mois par mois avec regression lineaire. Calcule la pente (slope), le coefficient R² (fiabilite), identifie le meilleur et pire mois, et determine la tendance globale (GROWING, DECLINING, STABLE).

**Parametre optionnel :** `months` = nombre de mois a analyser (defaut: 6)

**Resultat attendu :**
```json
{
  "monthlyProfits": [
    {"month": "SEPTEMBER 2025", "income": 3200.00, "expenses": 500.00, "netProfit": 2700.00, "profitMargin": 84.37}
  ],
  "overallGrowthRate": 25.50,
  "overallTrend": "GROWING",
  "bestMonth": "DECEMBER 2025",
  "bestMonthProfit": 2800.00,
  "worstMonth": "JANUARY 2026",
  "worstMonthProfit": -400.00,
  "averageMonthlyProfit": 1200.00,
  "regression": {
    "slope": 150.25,
    "intercept": 800.00,
    "rSquared": 0.72,
    "interpretation": "Forte tendance a la hausse — rentabilite en nette amelioration"
  }
}
```

### 10e. Categorisation Intelligente — `METIER AVANCE — IA`

**Method:** `POST`
**URL:** `http://localhost:8085/api/ai/accounting/categorize`
**Auth:** Bearer Token
**Body** (raw → JSON):
```json
{
  "description": "Achat de semences de ble pour la plantation de printemps"
}
```

**Ce que ca fait :** Algorithme NLP (Natural Language Processing) qui analyse la description textuelle d'une transaction et suggere automatiquement la categorie la plus probable. Utilise un systeme de mots-cles ponderes avec score de confiance pour chaque categorie.

**Testez avec differentes descriptions :**
```json
{"description": "Reparation du tracteur John Deere"}
```
→ Resultat: EQUIPMENT

```json
{"description": "Salaire des ouvriers agricoles"}
```
→ Resultat: LABOR

```json
{"description": "Livraison de 2 tonnes de tomates au marche de gros"}
```
→ Resultat: TRANSPORT

**Resultat attendu :**
```json
{
  "inputDescription": "Achat de semences de ble pour la plantation de printemps",
  "suggestedCategory": "SEEDS",
  "confidenceScore": 28.57,
  "allScores": [
    {"category": "SEEDS", "score": 28.57, "matchedKeywords": ["semence", "plantation"]},
    {"category": "FERTILIZER", "score": 0.0, "matchedKeywords": []},
    {"category": "IRRIGATION", "score": 0.0, "matchedKeywords": []}
  ]
}
```

### 10f. Alertes Budget Predictives — `METIER AVANCE — IA`

**Method:** `GET`
**URL:** `http://localhost:8085/api/ai/accounting/budget/predictive-alerts`
**Auth:** Bearer Token

**Ce que ca fait :** Algorithme predictif qui analyse le taux de consommation quotidien (daily burn rate) de chaque budget et projette si vous allez depasser le budget a la fin du mois. Calcule la date estimee d'epuisement et attribue une severite (CRITICAL, HIGH, MEDIUM, LOW).

**Condition pour avoir des resultats :** Il faut avoir des budgets actifs (Step 6) et des depenses qui avancent plus vite que prevu.

**Resultat attendu :**
```json
{
  "alerts": [
    {
      "category": "FERTILIZER",
      "budgetedAmount": 700.00,
      "spentSoFar": 500.00,
      "dailyBurnRate": 29.41,
      "projectedMonthEnd": 850.00,
      "projectedOverspend": 150.00,
      "daysRemaining": 11,
      "estimatedExhaustionDate": "2026-02-24",
      "severity": "HIGH",
      "recommendation": "Reduisez les depenses de FERTILIZER de 21% pour rester dans le budget."
    }
  ],
  "totalAlerts": 1
}
```

---

## STEP 11: AI Epargne (Savings AI) — `METIER AVANCE`

> **Prerequis :** Avoir un compte epargne avec un objectif (Step 3) et des transactions sur plusieurs mois (Step 4).

### 11a. Prediction d'Atteinte de l'Objectif — `METIER AVANCE — IA`

**Method:** `GET`
**URL:** `http://localhost:8085/api/ai/savings/goal/predict-achievement`
**Auth:** Bearer Token

**Ce que ca fait :** Algorithme predictif qui calcule une moyenne ponderee de votre epargne mensuelle sur les 7 derniers mois (les mois recents ont plus de poids) pour predire, objectif par objectif, quand chacun sera atteint. Tient compte du mode d'allocation (priorite, % fixe, proportionnel) pour estimer la contribution mensuelle de chaque goal.

**Donnees source :** Basee sur vos vraies transactions d'epargne (`savings_transaction`) et votre configuration de goals (priority, customAllocationPercentage). `monthsAnalyzed` indique combien de mois avec des transactions reelles ont ete trouves. `dataExplanation` decrit la methode utilisee.

**Resultat attendu :**
```json
{
  "currentBalance": 3800.00,
  "averageMonthlySavings": 500.00,
  "monthsAnalyzed": 5,
  "dataExplanation": "Moyenne ponderee sur 5 mois avec transactions reelles (poids croissants : mois recents comptent davantage)",
  "goalPredictions": [
    {
      "goalId": "...",
      "goalName": "Achat nouveau tracteur",
      "priority": 1,
      "allocationMode": "AUTO_PRIORITY",
      "customAllocationPct": null,
      "targetAmount": 10000.00,
      "currentAmount": 2923.08,
      "remaining": 7076.92,
      "progressPercentage": 29.23,
      "targetDate": "2027-06-01",
      "estimatedMonthlyContribution": 350.00,
      "estimatedMonthsToComplete": 21,
      "estimatedCompletionDate": "2028-01-24",
      "probability": 65.0,
      "status": "AT_RISK",
      "statusDetail": "Au rythme actuel, completion prevue en JAN 2028 — apres la date cible JUN 2027."
    },
    {
      "goalId": "...",
      "goalName": "Systeme irrigation goutte",
      "priority": 0,
      "allocationMode": "AUTO_PROPORTIONAL",
      "customAllocationPct": null,
      "targetAmount": 3000.00,
      "currentAmount": 876.92,
      "remaining": 2123.08,
      "progressPercentage": 29.23,
      "targetDate": "2026-09-01",
      "estimatedMonthlyContribution": 150.00,
      "estimatedMonthsToComplete": 15,
      "estimatedCompletionDate": "2027-06-24",
      "probability": 35.0,
      "status": "AT_RISK",
      "statusDetail": "Date cible SEP 2026 trop proche du rythme actuel."
    }
  ],
  "scenarios": [
    {"name": "Rythme actuel", "monthlySavings": 500.00, "achievementDate": "2028-01-24", "monthsRequired": 21, "description": "Completion du dernier objectif actif au rythme actuel"},
    {"name": "Scenario ambitieux (+50%)", "monthlySavings": 750.00, "achievementDate": "2027-04-24", "monthsRequired": 13, "description": "..."},
    {"name": "Scenario prudent (-25%)", "monthlySavings": 375.00, "achievementDate": "2028-09-24", "monthsRequired": 29, "description": "..."}
  ]
}
```

**Comment lire la reponse :**
- `monthsAnalyzed` → mois avec de vraies transactions dans `savings_transaction` (pas invente)
- `estimatedMonthlyContribution` → part mensuelle qui va a CE goal selon son mode d'allocation
- `status` : `ON_TRACK` (va atteindre avant la date cible), `AT_RISK` (retard probable), `ACHIEVED` (deja atteint), `BLOCKED` (epargne nulle)
- `scenarios` → date d'atteinte du DERNIER goal actif (le plus long)

### 11b. Plan d'Epargne Intelligent — `METIER AVANCE — IA`

**Method:** `GET`
**URL:** `http://localhost:8085/api/ai/savings/plan/smart`
**Auth:** Bearer Token

**Ce que ca fait :** Genere un plan d'epargne personnalise mois par mois en analysant les patterns saisonniers de vos revenus et depenses sur 12 mois. Les mois ou vos revenus sont plus eleves, il recommande d'epargner davantage. Chaque mois a une note saisonniere (favorable, normal, difficile).

**Resultat attendu :**
```json
{
  "goalAmount": 8000.00,
  "currentBalance": 3000.00,
  "remaining": 5000.00,
  "recommendedMonthlySavings": 416.67,
  "planDurationMonths": 12,
  "monthlyPlan": [
    {"month": "MARCH 2026", "recommendedSavings": 520.00, "projectedBalance": 3520.00, "progressPercentage": 44.0, "seasonalNote": "Mois favorable — revenus eleves, epargnez davantage"},
    {"month": "APRIL 2026", "recommendedSavings": 350.00, "projectedBalance": 3870.00, "progressPercentage": 48.37, "seasonalNote": "Mois difficile — depenses elevees, montant reduit"}
  ]
}
```

### 11c. Score de Risque de Retrait — `METIER AVANCE — AIDE A LA DECISION`

**Method:** `POST`
**URL:** `http://localhost:8085/api/ai/savings/withdrawal/risk-assessment`
**Auth:** Bearer Token
**Body** (raw → JSON):
```json
{
  "amount": 2000.00
}
```

**Ce que ca fait :** Evalue le risque d'un retrait AVANT de le faire via 4 facteurs ponderes :
- **Impact sur l'objectif** (35%) — Recul en % vers l'objectif
- **Couverture des depenses** (25%) — Mois de depenses couverts apres retrait
- **Analyse du tampon** (20%) — % de l'epargne retire
- **Historique d'epargne** (20%) — Tendance depots vs retraits

Score de 1 (faible risque) a 10 (risque critique).

**Testez avec differents montants :**
- `{"amount": 100.00}` → Risque LOW
- `{"amount": 2000.00}` → Risque MEDIUM/HIGH
- `{"amount": 2900.00}` → Risque CRITICAL

**Resultat attendu :**
```json
{
  "withdrawalAmount": 2000.00,
  "riskScore": 6,
  "riskLevel": "HIGH",
  "recommendation": "Envisagez un montant inferieur. Ce retrait impacte significativement votre securite financiere.",
  "goalImpact": {"name": "Impact sur l objectif", "score": 6, "weight": 0.35},
  "expenseCoverage": {"name": "Couverture des depenses", "score": 4, "weight": 0.25},
  "bufferAnalysis": {"name": "Analyse du tampon", "score": 8, "weight": 0.2},
  "savingsHistory": {"name": "Historique d epargne", "score": 4, "weight": 0.2},
  "warnings": ["Vous retirez plus de 60% de votre epargne."]
}
```

### 11d. Calculateur de Fonds d'Urgence — `METIER AVANCE — AIDE A LA DECISION`

**Method:** `GET`
**URL:** `http://localhost:8085/api/ai/savings/emergency-fund`
**Auth:** Bearer Token

**Ce que ca fait :** Analyse vos depenses reelles mois par mois sur les 6 derniers mois (depuis `accounting_entry` type EXPENSE) pour calculer le fonds d'urgence recommande (3 mois minimum, 6 mois optimal). Determine votre niveau de protection (NONE, LOW, MODERATE, GOOD, EXCELLENT) et calcule combien epargner par mois pour atteindre chaque seuil.

**Donnees source :** Chaque mois de la periode est analyse separement — vous voyez exactement combien vous avez depense et combien d'ecritures existent pour chaque mois.

**Resultat attendu :**
```json
{
  "totalExpensesLast6Months": 8400.00,
  "monthsWithExpenseData": 5,
  "periodFrom": "2025-09-24",
  "periodTo": "2026-03-24",
  "monthlyExpenseDetails": [
    {"month": "SEPTEMBER 2025", "totalExpenses": 800.00, "numberOfEntries": 2},
    {"month": "OCTOBER 2025", "totalExpenses": 1500.00, "numberOfEntries": 3},
    {"month": "NOVEMBER 2025", "totalExpenses": 1200.00, "numberOfEntries": 2},
    {"month": "DECEMBER 2025", "totalExpenses": 1900.00, "numberOfEntries": 4},
    {"month": "JANUARY 2026", "totalExpenses": 1600.00, "numberOfEntries": 3},
    {"month": "FEBRUARY 2026", "totalExpenses": 1400.00, "numberOfEntries": 3}
  ],
  "averageMonthlyExpenses": 1400.00,
  "recommendedMinimum": 4200.00,
  "recommendedOptimal": 8400.00,
  "currentSavings": 3800.00,
  "deficit": 400.00,
  "surplusAboveOptimal": 0.00,
  "coverageMonths": 2.71,
  "protectionLevel": "MODERATE",
  "protectionPercentage": 45.24,
  "monthlyContributionNeeded3Months": 66.67,
  "monthlyContributionNeeded6Months": 383.33,
  "recommendation": "Protection moderee. Epargnez 67 DT/mois pour atteindre le minimum de 3 mois en 6 mois."
}
```

**Comment lire la reponse :**
- `monthlyExpenseDetails` → detail reel mois par mois depuis vos ecritures comptables (verifiable dans Step 6)
- `monthsWithExpenseData` → mois ou vous avez eu de vraies depenses (les mois sans ecritures sont ignores)
- `averageMonthlyExpenses` → `totalExpenses / 6 mois` (base fixe pour la regularite du calcul)
- `deficit` → montant manquant pour atteindre le minimum de 3 mois (0 si deja OK)
- `surplusAboveOptimal` → excedent au-dessus du seuil optimal de 6 mois (0 si pas encore atteint)
- Niveaux : `NONE` (<0.5 mois), `LOW` (0.5-1.5), `MODERATE` (1.5-3), `GOOD` (3-6), `EXCELLENT` (>6)

---

## STEP 12: Tableau de Bord Decisionnels (Cross-Module AI) — `METIER AVANCE`

> **Prerequis :** Avoir des donnees dans les 2 modules (comptabilite + epargne).

### 12a. Dashboard de Decision — `METIER AVANCE — AIDE A LA DECISION`

**Method:** `GET`
**URL:** `http://localhost:8085/api/ai/dashboard`
**Auth:** Bearer Token

**Ce que ca fait :** Tableau de bord IA unifie qui croise les donnees de comptabilite et d'epargne. Affiche le score de sante, les metriques cles, des insights intelligents (succes, alertes, avertissements) et une liste d'actions prioritaires personnalisees.

**Resultat attendu :**
```json
{
  "financialHealthScore": 72,
  "healthLevel": "GOOD",
  "monthlyIncome": 2100.00,
  "monthlyExpenses": 1400.00,
  "monthlySavings": 700.00,
  "savingsBalance": 3000.00,
  "profitabilityTrend": "GROWING",
  "emergencyFundCoverage": 2.14,
  "keyInsights": [
    {"icon": "HEALTH", "title": "Bonne sante financiere", "description": "Score: 72/100", "severity": "SUCCESS"},
    {"icon": "SAVINGS", "title": "Taux d epargne: 33%", "description": "Vous epargnez 700 DT par mois", "severity": "SUCCESS"},
    {"icon": "EMERGENCY", "title": "Fonds d urgence insuffisant", "description": "Couvre seulement 2.14 mois", "severity": "WARNING"}
  ],
  "prioritizedActions": [
    {"priority": 1, "action": "Constituez un fonds d urgence de 3 mois minimum", "impact": "Haute", "category": "SAVINGS"}
  ]
}
```

### 12b. Optimiseur de Flux de Tresorerie — `METIER AVANCE — IA`

**Method:** `GET`
**URL:** `http://localhost:8085/api/ai/dashboard/optimize-cashflow`
**Auth:** Bearer Token

**Ce que ca fait :** Algorithme d'optimisation qui analyse chaque categorie de depenses et propose des reductions basees sur les normes agricoles :
- Transport: -15% (regrouper les livraisons)
- Divers: -20% (meilleure planification)
- Main d'oeuvre: -10% (optimisation planning)
- Equipement: -10% (maintenance preventive)
- Engrais: -5% (analyse de sol)
- Semences: -5% (achat en gros)

Calcule l'allocation ideale d'epargne (20% des revenus) et le plafond de depenses.

**Resultat attendu :**
```json
{
  "currentMonthlyNet": 700.00,
  "optimizedMonthlyNet": 910.00,
  "potentialImprovement": 210.00,
  "suggestions": [
    {"priority": 1, "category": "TRANSPORT", "type": "REDUCE_EXPENSE", "currentAmount": 250.00, "suggestedAmount": 212.50, "potentialSaving": 37.50, "justification": "Optimisez les trajets et regroupez les livraisons pour reduire de 15%"},
    {"priority": 2, "category": "LABOR", "type": "REDUCE_EXPENSE", "currentAmount": 700.00, "suggestedAmount": 630.00, "potentialSaving": 70.00, "justification": "Optimisez la planification du travail pour gagner 10% d efficacite"}
  ],
  "idealSavingsAllocation": 420.00,
  "idealExpensesCeiling": 1680.00,
  "overallStrategy": "Votre flux est positif mais insuffisant pour une epargne optimale..."
}
```

---

## STEP 13: User Management — `CRUD`

### 13a. Get All Users — `CRUD`

**Method:** `GET`
**URL:** `http://localhost:8085/agri/users`
**Auth:** none needed

### 13b. Get User by ID — `CRUD`

**Method:** `GET`
**URL:** `http://localhost:8085/agri/users/{id}`
**Auth:** Bearer Token
(Replace `{id}` with the actual user ID from Step 1)

### 13c. Check if Email Exists — `CRUD`

**Method:** `GET`
**URL:** `http://localhost:8085/agri/users/email/farmer@test.com/exists`
**Auth:** none needed

### 13d. Update User — `CRUD`

**Method:** `PUT`
**URL:** `http://localhost:8085/agri/users/{id}`
**Auth:** Bearer Token
**Body** (raw → JSON):
```json
{
  "email": "farmer@test.com",
  "firstName": "Ahmed",
  "lastName": "Ben Ali Updated",
  "phoneNumber": "87654321",
  "address": "Sfax, Tunisia"
}
```

### 13e. Delete User — `CRUD`

**Method:** `DELETE`
**URL:** `http://localhost:8085/agri/users/{id}`
**Auth:** Bearer Token

---

## Common Errors & Fixes

| Error Code | Meaning | Fix |
|------------|---------|-----|
| **401** | Not authenticated | Add your Bearer Token in the Authorization tab |
| **403** | Not authorized | Your user doesn't have the right role (need FARMER) |
| **404** | Not found | Check the URL, or the item with that ID doesn't exist |
| **400** | Bad request | Check your JSON body - something is missing or wrong |
| **500** | Server error | Check your Spring Boot console for the error details |

## Quick Reference: All Endpoints

| # | Method | URL | Auth? | Type |
|---|--------|-----|-------|------|
| 1 | POST | `/agri/users` | No | CRUD |
| 2 | POST | `/agri/auth/login` | No | CRUD |
| 3 | POST | `/api/savings/accounts` | Yes | CRUD |
| 4 | GET | `/api/savings/accounts/me` | Yes | CRUD |
| 5 | PUT | `/api/savings/accounts/me` | Yes | CRUD |
| 6 | DELETE | `/api/savings/accounts/me` | Yes | CRUD |
| 7 | POST | `/api/savings/transactions` | Yes | CRUD |
| 8 | GET | `/api/savings/transactions` | Yes | CRUD |
| 9 | GET | `/api/savings/transactions/{id}` | Yes | CRUD |
| 10 | DELETE | `/api/savings/transactions/{id}` | Yes (ADMIN) | CRUD |
| 11 | POST | `/api/savings/goals` | Yes | CRUD |
| 12 | GET | `/api/savings/goals` | Yes | CRUD |
| 13 | GET | `/api/savings/goals/{goalId}` | Yes | CRUD |
| 14 | PUT | `/api/savings/goals/{goalId}` | Yes | CRUD |
| 14b | PATCH | `/api/savings/goals/{goalId}/priority` | Yes | METIER AVANCE |
| 15 | DELETE | `/api/savings/goals/{goalId}` | Yes | CRUD |
| 16 | POST | `/api/accounting/entries` | Yes | CRUD |
| 17 | GET | `/api/accounting/entries` | Yes | CRUD |
| 18 | GET | `/api/accounting/entries/{id}` | Yes | CRUD |
| 19 | PUT | `/api/accounting/entries/{id}` | Yes | CRUD |
| 20 | DELETE | `/api/accounting/entries/{id}` | Yes | CRUD |
| 21 | POST | `/api/accounting/budgets` | Yes | CRUD |
| 22 | GET | `/api/accounting/budgets` | Yes | CRUD |
| 23 | GET | `/api/accounting/budgets/{id}` | Yes | CRUD |
| 24 | PUT | `/api/accounting/budgets/{id}` | Yes | CRUD |
| 25 | DELETE | `/api/accounting/budgets/{id}` | Yes | CRUD |
| 26 | GET | `/api/accounting/summary` | Yes | BESOIN FONCTIONNEL |
| 27 | GET | `/api/accounting/budget-vs-actual` | Yes | BESOIN FONCTIONNEL |
| 28 | GET | `/api/accounting/spending-breakdown` | Yes | BESOIN FONCTIONNEL |
| 29 | GET | `/api/accounting/cashflow/forecast` | Yes | BESOIN FONCTIONNEL |
| 30 | GET | `/api/accounting/alerts/overspending` | Yes | BESOIN FONCTIONNEL |
| 31 | GET | `/api/accounting/anomalies` | Yes | BESOIN FONCTIONNEL |
| 32 | GET | `/api/savings/accounts/goal/progress` | Yes | BESOIN FONCTIONNEL |
| 33 | GET | `/api/savings/accounts/summary/monthly` | Yes | BESOIN FONCTIONNEL |
| 34 | GET | `/api/savings/accounts/alerts` | Yes | BESOIN FONCTIONNEL |
| 35 | POST | `/api/savings/accounts/simulate/withdraw` | Yes | BESOIN FONCTIONNEL |
| 36 | GET | `/api/savings/accounts/recommendation` | Yes | BESOIN FONCTIONNEL |
| 37 | GET | `/api/savings/accounts/statement` | Yes | BESOIN FONCTIONNEL |
| 38 | GET | `/api/ai/accounting/health-score` | Yes | METIER AVANCE — IA |
| 39 | GET | `/api/ai/accounting/forecast/expenses` | Yes | METIER AVANCE — IA |
| 40 | POST | `/api/ai/accounting/simulate/what-if` | Yes | METIER AVANCE — AIDE DECISION |
| 41 | GET | `/api/ai/accounting/trends/profitability` | Yes | METIER AVANCE — IA |
| 42 | POST | `/api/ai/accounting/categorize` | Yes | METIER AVANCE — IA |
| 43 | GET | `/api/ai/accounting/budget/predictive-alerts` | Yes | METIER AVANCE — IA |
| 44 | GET | `/api/ai/savings/goal/predict-achievement` | Yes | METIER AVANCE — IA |
| 45 | GET | `/api/ai/savings/plan/smart` | Yes | METIER AVANCE — IA |
| 46 | POST | `/api/ai/savings/withdrawal/risk-assessment` | Yes | METIER AVANCE — AIDE DECISION |
| 47 | GET | `/api/ai/savings/emergency-fund` | Yes | METIER AVANCE — AIDE DECISION |
| 48 | GET | `/api/ai/dashboard` | Yes | METIER AVANCE — AIDE DECISION |
| 49 | GET | `/api/ai/dashboard/optimize-cashflow` | Yes | METIER AVANCE — IA |
| 50 | GET | `/agri/users` | No | CRUD |
| 51 | GET | `/agri/users/{id}` | Yes | CRUD |
| 52 | PUT | `/agri/users/{id}` | Yes | CRUD |
| 53 | DELETE | `/agri/users/{id}` | Yes | CRUD |
