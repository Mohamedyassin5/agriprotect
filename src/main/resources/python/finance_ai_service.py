"""
Finance AI Service — port 8002
Serves ML predictions for:
  - POST /predict/financial-risk   (Module 2 — Accounting)
  - POST /predict/savings-alert    (Module 3 — Savings)

Start: uvicorn finance_ai_service:app --host 0.0.0.0 --port 8002
Requires: pip install fastapi uvicorn scikit-learn joblib numpy
"""

import os, sys
import numpy as np
import joblib
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

# ── locate models dir (../models relative to this file)
BASE = os.path.dirname(os.path.abspath(__file__))
MODELS_DIR = os.path.join(BASE, "..", "models")

def load(name):
    path = os.path.join(MODELS_DIR, name)
    if not os.path.exists(path):
        raise RuntimeError(
            f"Model file not found: {path}\n"
            "Please run train_finance_models.py first."
        )
    return joblib.load(path)

# ── load at startup
try:
    risk_model   = load("financial_risk_model.pkl")
    risk_scaler  = load("financial_risk_scaler.pkl")
    risk_pca     = load("financial_risk_pca.pkl")
    risk_encoder = load("financial_risk_encoder.pkl")

    sav_model    = load("savings_alert_model.pkl")
    sav_scaler   = load("savings_alert_scaler.pkl")
    sav_pca      = load("savings_alert_pca.pkl")
    sav_encoder  = load("savings_alert_encoder.pkl")
    print("All finance models loaded.")
except RuntimeError as e:
    print(f"ERROR: {e}", file=sys.stderr)
    sys.exit(1)

app = FastAPI(title="AgriProtect Finance AI", version="1.0")
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])

# ─────────────────────────────────────────────
# Schemas
# ─────────────────────────────────────────────
class FinancialRiskRequest(BaseModel):
    revenue: float
    expenses: float
    debtToEquity: float
    netProfit: float
    avgTemperature: float = 28.0
    rainfall: float = 400.0
    droughtIndex: float = 3.0
    floodRiskScore: float = 2.0

class SavingsAlertRequest(BaseModel):
    revenue: float
    expenses: float
    loanAmount: float = 0.0
    droughtIndex: float = 3.0
    floodRiskScore: float = 2.0
    policySupportScore: float = 5.0

# ─────────────────────────────────────────────
# Endpoints
# ─────────────────────────────────────────────
@app.get("/health")
def health():
    return {"status": "ok", "service": "finance-ai"}

@app.post("/predict/financial-risk")
def predict_financial_risk(req: FinancialRiskRequest):
    X = np.array([[req.revenue, req.expenses, req.debtToEquity, req.netProfit,
                   req.avgTemperature, req.rainfall, req.droughtIndex, req.floodRiskScore]])
    X_scaled = risk_scaler.transform(X)
    X_pca    = risk_pca.transform(X_scaled)
    pred     = risk_model.predict(X_pca)[0]
    proba    = risk_model.predict_proba(X_pca)[0]

    label = risk_encoder.inverse_transform([pred])[0]
    classes = list(risk_encoder.classes_)
    proba_map = {classes[i]: round(float(proba[i]) * 100, 1) for i in range(len(classes))}

    interpretations = {
        "High":   "Risque financier ÉLEVÉ — des mesures correctives urgentes sont nécessaires.",
        "Medium": "Risque financier MODÉRÉ — surveillance renforcée recommandée.",
        "Low":    "Risque financier FAIBLE — situation financière saine."
    }

    return {
        "riskLevel":       label,
        "confidence":      round(float(proba[pred]) * 100, 1),
        "probabilities":   proba_map,
        "interpretation":  interpretations.get(label, ""),
        "alertColor":      {"High": "#c0392b", "Medium": "#e07b39", "Low": "#40916c"}.get(label, "#888")
    }

@app.post("/predict/savings-alert")
def predict_savings_alert(req: SavingsAlertRequest):
    # Also compute formula-based values for context
    rec_sav = (0.12 * req.revenue - 0.05 * req.expenses - 0.04 * req.loanAmount
               - 10.0 * req.droughtIndex - 8.0 * req.floodRiskScore
               + 2.0 * req.policySupportScore)
    sav_cap = (0.30 * max(req.revenue - req.expenses, 0) - 0.08 * req.loanAmount
               - 15.0 * req.droughtIndex - 12.0 * req.floodRiskScore
               + 3.0 * req.policySupportScore)
    rec_sav = max(rec_sav, 1.0)
    ratio = sav_cap / rec_sav

    X = np.array([[req.revenue, req.expenses, req.loanAmount,
                   req.droughtIndex, req.floodRiskScore, req.policySupportScore]])
    X_scaled = sav_scaler.transform(X)
    X_pca    = sav_pca.transform(X_scaled)
    pred     = sav_model.predict(X_pca)[0]
    proba    = sav_model.predict_proba(X_pca)[0]

    label = sav_encoder.inverse_transform([pred])[0]
    classes = list(sav_encoder.classes_)
    proba_map = {classes[i]: round(float(proba[i]) * 100, 1) for i in range(len(classes))}

    interpretations = {
        "Insufficient": "Capacité d'épargne INSUFFISANTE — réduire les charges ou augmenter les revenus.",
        "Moderate":     "Capacité d'épargne MODÉRÉE — des efforts supplémentaires sont recommandés.",
        "Good":         "Capacité d'épargne BONNE — continuez sur cette lancée !"
    }

    return {
        "alertStatus":         label,
        "confidence":          round(float(proba[pred]) * 100, 1),
        "probabilities":       proba_map,
        "recommendedSavings":  round(rec_sav, 2),
        "savingCapacity":      round(sav_cap, 2),
        "capacityRatio":       round(ratio, 3),
        "interpretation":      interpretations.get(label, ""),
        "alertColor":          {"Insufficient": "#c0392b", "Moderate": "#e07b39", "Good": "#40916c"}.get(label, "#888")
    }
