# Quick Test — AgriProtect

Base URL : `http://localhost:8085`
Auth : **Bearer Token** (Authorization tab → Bearer Token)

---

## 1. Créer un utilisateur
`POST /agri/users`
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

---

## 2. Login → copier le token
`POST /agri/auth/login`
```json
{
  "email": "farmer@test.com",
  "password": "password123"
}
```
> Copier le `token` reçu et le coller dans **Authorization → Bearer Token** de toutes les requêtes suivantes.

---

## 3. Créer un compte épargne
`POST /api/savings/accounts`
```json
{
  "accountName": "Epargne agricole",
  "monthlySavingsTarget": 500.00
}
```

---

## 4. Faire un dépôt
`POST /api/savings/transactions`
```json
{
  "type": "DEPOSIT",
  "amount": 1500.00,
  "description": "Epargne mensuelle"
}
```

---

## 5. Créer un objectif d'épargne
`POST /api/savings/goals`
```json
{
  "title": "Achat tracteur",
  "targetAmount": 10000.00,
  "targetDate": "2025-12-31",
  "priority": 1
}
```

---

## 6. Créer une entrée comptable (dépense)
`POST /api/accounting/entries`
```json
{
  "entryType": "EXPENSE",
  "category": "EQUIPMENT",
  "amount": 200.00,
  "description": "Achat engrais",
  "entryDate": "2025-03-01"
}
```

---

## 7. Créer un budget
`POST /api/accounting/budgets`
```json
{
  "category": "EQUIPMENT",
  "budgetAmount": 1000.00,
  "periodType": "MONTHLY",
  "startDate": "2025-03-01",
  "endDate": "2025-03-31"
}
```

---

## Endpoints de vérification rapide

| Action | Méthode | URL |
|--------|---------|-----|
| Voir mon compte épargne | GET | `/api/savings/accounts/me` |
| Voir mes transactions | GET | `/api/savings/transactions` |
| Voir mes objectifs | GET | `/api/savings/goals` |
| Voir mes entrées comptables | GET | `/api/accounting/entries` |
| Voir mes budgets | GET | `/api/accounting/budgets` |
| Dashboard décision | GET | `/api/accounting/dashboard` |
