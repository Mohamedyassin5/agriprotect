from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import joblib
import numpy as np
from pathlib import Path

app = FastAPI(title="Agri AI Service")

# Chemin vers resources/crop_ai.saved_models
BASE_DIR = Path(__file__).resolve().parent
MODELS_DIR = (BASE_DIR / ".." / "crop_ai.saved_models").resolve()

# Charger les artefacts
scaler = joblib.load(MODELS_DIR / "scaler.pkl")
label_encoder = joblib.load(MODELS_DIR / "label_encoder.pkl")

rf_model = joblib.load(MODELS_DIR / "random_forest_model.pkl")
svm_model = joblib.load(MODELS_DIR / "svm_model.pkl")
xgb_model = joblib.load(MODELS_DIR / "xgboost_model.pkl")

FEATURES = ["N","P","K","temperature","humidity","ph","rainfall","Soil_Fertility_Index"]

class PredictRequest(BaseModel):
    N: float
    P: float
    K: float
    temperature: float
    humidity: float
    ph: float
    rainfall: float
    Soil_Fertility_Index: float
    model: str = "xgb"  # xgb | rf | svm
    top_k: int = 1

@app.get("/health")
def health():
    return {"status": "ok"}

@app.post("/predict")
def predict(req: PredictRequest):
    try:
        x = np.array([[getattr(req, f) for f in FEATURES]], dtype=float)
        x_scaled = scaler.transform(x)

        if req.model == "rf":
            probs = rf_model.predict_proba(x_scaled)[0]
        elif req.model == "svm":
            probs = svm_model.predict_proba(x_scaled)[0]
        elif req.model == "xgb":
            probs = xgb_model.predict_proba(x_scaled)[0]
        else:
            raise HTTPException(status_code=400, detail="model must be xgb|rf|svm")

        top_indices = np.argsort(probs)[-req.top_k:][::-1]
        crops = label_encoder.inverse_transform(top_indices).tolist()
        
        return {"recommended_crops": crops, "model_used": req.model}

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
