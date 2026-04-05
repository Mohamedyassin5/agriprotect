# 🌾 AgriProtect — Agricultural Insurance & Microfinance & Farm Management Platform

> A full-featured Spring Boot backend for agricultural insurance management, AI-powered crop recommendations, risk detection, financial analytics, and more.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Features](#features)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [API Endpoints](#api-endpoints)
- [External Services](#external-services)
- [Security](#security)

---

## Overview

**AgriProtect** is a Spring Boot REST API platform designed for agricultural management. It enables farmers to subscribe to insurance policies, get AI-powered risk assessments and crop recommendations, manage savings, track budgets, and apply for credit — all in one place.

The platform integrates with several external services including:
- **Groq LLM API** (LLaMA 3.3) for AI insights and recommendations
- **Stripe** for payment processing
- **Open-Meteo** for weather data
- **A custom Python AI microservice** for crop/risk ML models

---

## Tech Stack

| Layer         | Technology                          |
|---------------|-------------------------------------|
| Language      | Java 21                             |
| Framework     | Spring Boot 4.0.2                   |
| Security      | Spring Security + JWT (JJWT 0.11.5) |
| Database      | MySQL (JPA / Hibernate)             |
| API Docs      | SpringDoc OpenAPI (Swagger UI)      |
| Payments      | Stripe Java SDK 28.0.0              |
| PDF           | iTextPDF, OpenPDF, Apache PDFBox    |
| QR Codes      | ZXing 3.5.3                         |
| Email         | Spring Mail (Gmail SMTP)            |
| AI / LLM      | Groq API (LLaMA 3.3-70b)            |
| Reactive      | Spring WebFlux (WebClient)          |
| Build Tool    | Maven                               |

---

## Features

### 🛡️ Insurance Management (Phase 1)
- AI-powered premium estimation based on farmer profile
- Full insurance subscription lifecycle (PENDING → SIGNED → ACTIVE)
- Coverage types: STANDARD, PREMIUM, BASIC
- Payment mode selection (monthly, quarterly, yearly)
- Digital signature via secure token link
- Insurance certificate PDF generation (FR / EN / AR)
- Payment invoice PDF download
- Overdue detection with automated penalty handling
- Policy suspension after 15 days overdue
- Regularization flow for suspended policies
- Farmer dashboard with policy & payment summary
- Admin panel: all policies, stats, overdue overview

### 🌱 Crop Management & AI Recommendations
- AI-powered crop recommendation engine (XGBoost ML model)
- Crop reference library (admin-managed)
- Farmer crop tracking
- Configurable number of recommendations

### ⚠️ Risk & Sinistre (Claim) Management
- Automated risk detection with weather integration (Open-Meteo)
- Scheduled risk checks (6 AM, 12 PM, 6 PM)
- Daily/monthly quota enforcement
- Risk audit log
- Sinistre (incident/claim) creation and tracking
- AI risk assessment scoring

### 💰 Financial Module
- Savings accounts & savings goals
- Savings transactions tracking
- Budget management
- Credit & loan application (`DemandeCredit`)
- Credit approval workflow
- Accounting entries & profitability analysis
- AI-powered accounting insights
- AI-powered savings analysis
- Decision dashboard

### 👤 User Management
- JWT-based registration & login
- Role-based access control (`FARMER`, `ADMIN`)
- Face verification service integration
- Password reset via email code
- Profile management

### 📧 Notifications
- Email confirmation on insurance activation
- Payment reminder emails (2 days before due)
- Overdue and suspension notifications
- Scheduled email reminders (Spring `@Scheduled`)

---

## Project Structure

```
src/main/java/tn/esprit/agri/
├── AgriApplication.java          # Entry point
├── ai/                           # AI client, DTOs & services
│   ├── client/                   # HTTP clients for Python AI service
│   ├── dto/                      # AI request/response DTOs
│   └── service/                  # AI service interfaces & impls
├── config/                       # Spring configuration beans
├── controlleurs/                 # REST controllers
│   ├── Phase1Controller.java     # Insurance & payments
│   ├── CropController.java       # Crop management
│   ├── UserController.java       # User profile
│   ├── BudgetController.java     # Budget
│   ├── CreditController.java     # Credit
│   ├── SavingsAccountController.java
│   ├── SavingsGoalController.java
│   ├── SavingsTransactionController.java
│   ├── AccountingEntryController.java
│   ├── AccountingAIController.java
│   ├── AccountingAnalyticsController.java
│   ├── SavingsAIController.java
│   ├── AuditController.java
│   ├── DecisionDashboardController.java
│   ├── StripeWebhookController.java
│   ├── DemandeCreditController.java
│   ├── AdminCropReferenceController.java
│   ├── auth/                     # Auth endpoints (register, login)
│   ├── crop/                     # Crop sub-controllers
│   ├── sinistre/                 # Risk & claim controllers
│   ├── user/                     # User sub-controllers
│   └── assistant/                # AI assistant endpoints
├── DTO/                          # Shared DTOs
├── dto_savings_accountability/   # Savings & accounting DTOs
├── entities/                     # JPA entities
│   ├── User.java
│   ├── Insurance.java
│   ├── Payment.java
│   ├── Crop.java / CropReference.java
│   ├── Risque.java / Sinistre.java
│   ├── SavingsAccount.java / SavingsGoal.java / SavingsTransaction.java
│   ├── Budget.java / Credit.java / DemandeCredit.java
│   ├── AccountingEntry.java / AnalyseRentabilite.java
│   ├── AuditLog.java / Echeance.java
│   └── enums/                    # Role, InsuranceStatus, CoverageType, etc.
├── exception/                    # Global exception handler
├── repositories/                 # Spring Data JPA repositories
├── security/                     # JWT filter & service
├── services/                     # Business logic interfaces & implementations
└── utils/                        # Utility classes
```

---

## Getting Started

### Prerequisites

| Requirement       | Version   |
|-------------------|-----------|
| Java              | 21+       |
| Maven             | 3.8+      |
| MySQL             | 8+        |
| Python (AI svc)   | 3.9+ (optional) |

### 1. Clone the Repository

```bash
git clone https://github.com/Mohamedyassin5/agriprotect.git
cd agriprotect
```

### 2. Set Up the Database

Create a MySQL database (auto-created if it doesn't exist):

```sql
CREATE DATABASE agri;
```

### 3. Configure the Application

Copy and edit `src/main/resources/application.properties` — see [Configuration](#configuration) below.

### 4. Run the Application

```bash
./mvnw spring-boot:run
```

Or on Windows:

```powershell
mvnw.cmd spring-boot:run
```

The server starts on **http://localhost:8081**

### 5. Access Swagger UI

```
http://localhost:8081/swagger-ui.html
```

---

## Configuration

Edit `src/main/resources/application.properties`:

```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/agri?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=YOUR_DB_PASSWORD

# JWT
jwt.secret=YOUR_SECURE_RANDOM_SECRET
jwt.expirationMs=3600000

# Email (Gmail SMTP)
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password

# Stripe
stripe.api.key=sk_test_...
stripe.webhook.secret=whsec_...

# Groq AI
groq.api.key=gsk_...
groq.api.model=llama-3.3-70b-versatile

# Python AI Microservice
agri.ai.base-url=http://localhost:8000

# Python Face Verification Service
agri.face.base-url=http://localhost:8001

# Weather API (Open-Meteo, no key required)
weather.default.latitude=36.8065
weather.default.longitude=10.1686
```

> ⚠️ **Never commit real secrets to version control.** Use environment variables or a `.env` file approach for production.

---

## API Endpoints

### Authentication
| Method | Endpoint             | Description       |
|--------|----------------------|-------------------|
| POST   | `/auth/register`     | Register new user |
| POST   | `/auth/login`        | Login, get JWT    |
| POST   | `/auth/reset-*`      | Password reset    |

### Insurance (`/agri/phase1`)
| Method | Endpoint                                  | Role         | Description                        |
|--------|-------------------------------------------|--------------|------------------------------------|
| GET    | `/agri/phase1/estimate`                   | FARMER       | Estimate insurance premium         |
| GET    | `/agri/phase1/estimate/ai-complete`       | FARMER       | Full AI-based premium estimation   |
| POST   | `/agri/phase1/subscribe`                  | FARMER       | Subscribe to an insurance policy   |
| GET    | `/agri/phase1/my-insurances`              | FARMER       | List my active policies            |
| GET    | `/agri/phase1/{id}`                       | FARMER/ADMIN | Get policy details                 |
| DELETE | `/agri/phase1/{id}`                       | FARMER       | Cancel pending subscription        |
| GET    | `/agri/phase1/{id}/certificate.pdf`       | FARMER/ADMIN | Download certificate (FR/EN/AR)    |
| POST   | `/agri/phase1/{id}/sign/token`            | Public       | Sign policy via email token        |
| POST   | `/agri/phase1/pay/{insuranceId}`          | FARMER       | Initiate Stripe payment            |
| GET    | `/agri/phase1/my-payments`               | FARMER       | List my payment history            |
| GET    | `/agri/phase1/{insuranceId}/payments`     | FARMER/ADMIN | Payment history for a policy       |
| GET    | `/agri/phase1/{insuranceId}/invoice.pdf`  | FARMER       | Download payment invoice PDF       |
| POST   | `/agri/phase1/{id}/regularize`            | FARMER/ADMIN | Regularize suspended policy        |
| POST   | `/agri/phase1/{id}/regularize-payment`    | FARMER/ADMIN | Initiate regularization payment    |
| GET    | `/agri/phase1/dashboard`                  | FARMER       | Farmer dashboard summary           |
| GET    | `/agri/phase1/admin/insurances`           | ADMIN        | All policies (filterable by status)|
| GET    | `/agri/phase1/admin/stats`                | ADMIN        | Platform statistics                |
| GET    | `/agri/phase1/admin/overdue`              | ADMIN        | Overdue & suspended policies       |

### Crops & AI Recommendations
| Method | Endpoint             | Description                        |
|--------|----------------------|------------------------------------|
| GET    | `/crops/recommend`   | AI crop recommendations            |
| GET/POST/PUT/DELETE | `/crops/**` | CRUD operations for crops    |
| GET/POST | `/admin/crop-references/**` | Manage crop reference library |

### Risk & Claims
| Method | Endpoint             | Description                  |
|--------|----------------------|------------------------------|
| GET    | `/risques/**`        | Risk detection & management  |
| GET/POST | `/sinistres/**`    | Claim creation & tracking    |

### Financial
| Method | Endpoint                        | Description              |
|--------|---------------------------------|--------------------------|
| `/**`  | `/savings-accounts/**`          | Savings accounts         |
| `/**`  | `/savings-goals/**`             | Savings goals            |
| `/**`  | `/savings-transactions/**`      | Savings transactions     |
| `/**`  | `/budgets/**`                   | Budget management        |
| `/**`  | `/credits/**`                   | Credit management        |
| `/**`  | `/demande-credits/**`           | Loan applications        |
| `/**`  | `/accounting-entries/**`        | Accounting entries       |
| GET    | `/accounting/analytics/**`      | Financial analytics      |
| GET    | `/accounting/ai/**`             | AI accounting insights   |
| GET    | `/savings/ai/**`                | AI savings analysis      |
| GET    | `/decision-dashboard/**`        | Decision dashboard       |

### Users
| Method | Endpoint         | Description       |
|--------|------------------|-------------------|
| GET    | `/users/me`      | Get own profile   |
| PUT    | `/users/me`      | Update profile    |
| GET    | `/users/**`      | Admin user list   |

---

## External Services

| Service        | Purpose                         | URL / API                     |
|----------------|---------------------------------|-------------------------------|
| **Groq**       | LLM AI insights (LLaMA 3.3)     | `https://api.groq.com`        |
| **Stripe**     | Insurance payment processing    | `https://stripe.com`          |
| **Open-Meteo** | Weather data (free, no key)     | `https://api.open-meteo.com`  |
| **Python AI**  | Crop/risk ML models (XGBoost)   | `http://localhost:8000`       |
| **Face Svc**   | Face verification               | `http://localhost:8001`       |
| **Gmail SMTP** | Transactional emails            | `smtp.gmail.com:587`          |

---

## Security

- **JWT Authentication**: All protected endpoints require a `Bearer` token in the `Authorization` header.
- **Role-based access**: `FARMER` and `ADMIN` roles with `@PreAuthorize` guards.
- **Stripe Webhook**: Verified via Stripe signature header.
- **Token-based Signing**: Insurance documents use single-use, time-limited tokens.

### Getting a JWT Token

```bash
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "your@email.com", "password": "yourpassword"}'
```

Use the returned token in subsequent requests:

```bash
curl http://localhost:8081/agri/phase1/my-insurances \
  -H "Authorization: Bearer <your-token>"
```

---

## 📄 License

This project was developed as part of an academic project at **ESPRIT** (École Supérieure Privée d'Ingénierie et de Technologie), Tunisia.
