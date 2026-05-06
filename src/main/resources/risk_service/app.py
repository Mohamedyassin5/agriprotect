from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import joblib
import pandas as pd
import numpy as np
import os
import uvicorn
from fastapi.middleware.cors import CORSMiddleware

app = FastAPI(title="AgriProtect Risk Analyzer Service")

# CORS setup
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# Model loading
MODELS = {}
MODEL_DIR = os.path.dirname(os.path.abspath(__file__))

MODEL_FILES = {
    "dt": "DecisionTree_pipeline.pkl",
    "knn": "KNN_pipeline.pkl",
    "rf": "RandomForest_pipeline.pkl",
    "svm": "SVM_pipeline.pkl"
}


def load_models():
    for key, filename in MODEL_FILES.items():
        path = os.path.join(MODEL_DIR, filename)
        if os.path.exists(path):
            try:
                MODELS[key] = joblib.load(path)
                print(f"Loaded model {key} from {filename}")
            except Exception as e:
                print(f"Error loading {filename}: {e}")
        else:
            print(f"Model file {filename} not found in {MODEL_DIR}")

load_models()

class RiskInput(BaseModel):
    Region: str
    Enterprise_Size: str
    Revenue: float
    Expenses: float
    Loan_Amount: float
    Debt_to_Equity: float
    Avg_Temperature: float
    Rainfall: float
    Drought_Index: float
    Flood_Risk_Score: float
    Commodity_Price_Index: float
    Input_Cost_Index: float
    Policy_Support_Score: int
    Quarter: str
    Net_Profit: float
    Crop_ID: str
    model: str = "svm"

@app.post("/predict")
async def predict(data: RiskInput):
    model_key = data.model
    if model_key not in MODELS:
        raise HTTPException(status_code=404, detail=f"Model {model_key} not loaded")
    
    model = MODELS[model_key]
    
    # 1. Define the strict feature list in the correct order
    FEATURES = [
        "Region", "Enterprise_Size", "Revenue", "Expenses", "Loan_Amount", 
        "Debt_to_Equity", "Avg_Temperature", "Rainfall", "Drought_Index", 
        "Flood_Risk_Score", "Commodity_Price_Index", "Input_Cost_Index", 
        "Policy_Support_Score", "Quarter", "Net_Profit", "Crop_ID"
    ]

    try:
        input_dict = data.model_dump()
        
        # 2. Numeric String Mapping (The only format that satisfies both Scaler and Encoder)
        region_map = {"North": "0", "South": "1", "East": "2", "West": "3", "Central": "4", "Coastal": "5", "Tunis": "6"}
        size_map = {"Small": "0", "Medium": "1", "Large": "2"}
        quarter_map = {"Q1": "1", "Q2": "2", "Q3": "3", "Q4": "4"}
        crop_map = {"Wheat": "0", "Corn": "1", "Barley": "2", "Tomato": "3", "Olive": "4", "Pear": "5", "Potato": "6"}

        input_dict["Region"] = region_map.get(input_dict.get("Region"), "0")
        
        e_size = input_dict.get("Enterprise_Size")
        if isinstance(e_size, str) and e_size.isdigit():
            input_dict["Enterprise_Size"] = e_size
        else:
            input_dict["Enterprise_Size"] = size_map.get(e_size, "1")
            
        input_dict["Quarter"] = quarter_map.get(input_dict.get("Quarter"), "1")
        input_dict["Crop_ID"] = crop_map.get(input_dict.get("Crop_ID"), "0")

        # 3. Build DataFrame with exactly the expected columns
        df = pd.DataFrame([input_dict])
        
        if hasattr(model, "feature_names_in_"):
            expected = list(model.feature_names_in_)
            df = df.reindex(columns=expected).fillna("0")
        else:
            df = df.reindex(columns=FEATURES).fillna("0")

        # IMPORTANT: We do NOT convert to float here. 
        # We keep the categorical columns as strings ("0", "1") 
        # and the numeric columns will be converted automatically by the pipeline.
        
        print(f"--- Final Input Shape: {df.shape} ---")
        print(f"--- Final Data Sample: {df.iloc[0].to_dict()} ---")
        
        # 4. Predict
        prediction = model.predict(df)[0]



        
        # If the model has predict_proba, get confidence
        confidence = 1.0
        if hasattr(model, "predict_proba"):
            probs = model.predict_proba(df)[0]
            confidence = float(max(probs))


            
        return {
            "prediction": str(prediction),
            "confidence": confidence,
            "model_used": MODEL_FILES.get(model_key, model_key)
        }
    except Exception as e:
        import traceback
        print("!!! DETAILED ERROR TRACEBACK:")
        print(traceback.format_exc()) # This shows the exact line of failure
        raise HTTPException(status_code=500, detail=f"Prediction error: {str(e)}")



@app.get("/health")
async def health():
    return {"status": "ok", "models_loaded": list(MODELS.keys())}

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8003)
