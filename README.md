# AgriProtect – Agricultural Insurance & Microfinance Platform

## Overview
This project was developed as part of the PIDEV – [Votre Classe, ex: 3A10] Engineering Program at **Esprit School of Engineering** (Academic Year 2025–2026).

AgriProtect is a Spring Boot REST API platform designed for agricultural management. It enables farmers to subscribe to insurance policies, get AI-powered risk assessments and crop recommendations, manage savings, track budgets, and apply for credit — all in one place.

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
- Policy suspension and regularization flow

### 🌱 Crop Management & AI Recommendations
- AI-powered crop recommendation engine (XGBoost ML model)
- Crop reference library (admin-managed)
- Farmer crop tracking

### ⚠️ Risk & Sinistre (Claim) Management
- Automated risk detection with weather integration (Open-Meteo)
- Scheduled risk checks (6 AM, 12 PM, 6 PM)
- Sinistre (incident/claim) creation and tracking
- AI risk assessment scoring

### 💰 Financial Module
- Savings accounts, goals, and transactions tracking
- Budget management and Credit/loan application
- Accounting entries & AI-powered profitability analysis
- AI-powered savings analysis and decision dashboard

### 👤 User Management & Notifications
- JWT-based registration & login with Role-based access control (FARMER, ADMIN)
- Face verification service integration
- Email confirmations, payment reminders, and scheduled notifications

## Tech Stack
### Frontend
- *(If you have a frontend repository or tech, list it here: e.g., Angular, React.js)*

### Backend
- **Language**: Java 21
- **Framework**: Spring Boot 4.0.2
- **Security**: Spring Security + JWT (JJWT 0.11.5)
- **Database**: MySQL (JPA / Hibernate)
- **Payments**: Stripe Java SDK 28.0.0
- **AI / LLM**: Groq API (LLaMA 3.3-70b)
- **ML / External**: Python for custom AI risk/crop models

## Architecture
- **Monolithic Core:** Spring Boot backend handling all primary business logic, REST APIs, and database interactions.
- **Microservices Integration:** Dedicated Python microservices for XGBoost ML models and Face Verification.
- **External Services Integrations:** Stripe (Payment Gateway), Groq LLM (AI Insights), Open-Meteo (Weather Data).

## Contributors
- [Your Name] – [GitHub Link]
- [Team Member 2] – [GitHub Link]
- [Team Member 3] – [GitHub Link]

## Academic Context
Developed at **Esprit School of Engineering – Tunisia**
PIDEV – [Votre Classe, ex: 3A10] | 2025–2026

## Getting Started

### Prerequisites
- Java 21+
- Maven 3.8+
- MySQL 8+
- Python 3.9+ (optional for AI svc)

### 1. Clone the Repository
```bash
git clone https://github.com/Mohamedyassin5/agriprotect.git
cd agriprotect
```

### 2. Set Up the Database
```sql
CREATE DATABASE agri;
```

### 3. Configure the Application
Edit `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/agri?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=YOUR_DB_PASSWORD
# ... add JWT secret, Stripe keys, and Groq keys as required.
```

### 4. Run the Application
```bash
./mvnw spring-boot:run
```
The server starts on **http://localhost:8081**

### 5. Access Swagger UI
```
http://localhost:8081/swagger-ui.html
```

## Acknowledgments
- Special thanks to our academic supervisors at **Esprit School of Engineering**.
- Thanks to the open-source community for the frameworks and libraries used to build this platform.

---
*(Below are additional technical details for project maintainers)*

### Project Structure
```
src/main/java/tn/esprit/agri/
├── AgriApplication.java          # Entry point
├── ai/                           # AI client, DTOs & services
├── config/                       # Spring configuration beans
├── controlleurs/                 # REST controllers
├── DTO/                          # Shared DTOs
├── entities/                     # JPA entities
├── exception/                    # Global exception handler
├── repositories/                 # Spring Data JPA repositories
├── security/                     # JWT filter & service
├── services/                     # Business logic interfaces & implementations
└── utils/                        # Utility classes
```

### Security
- **JWT Authentication**: All protected endpoints require a `Bearer` token in the `Authorization` header.
- **Role-based access**: `FARMER` and `ADMIN` roles with `@PreAuthorize` guards.
- **Stripe Webhook**: Verified via Stripe signature header.
