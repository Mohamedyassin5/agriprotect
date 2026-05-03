@echo off
echo ============================================
echo  AgriProtect Finance AI Service — Setup
echo ============================================

set SCRIPT_DIR=%~dp0
set MODELS_DIR=%SCRIPT_DIR%..\models

echo.
echo [1/3] Installing dependencies...
pip install fastapi uvicorn scikit-learn joblib numpy xgboost --quiet

echo.
echo [2/3] Training models (first run only)...
cd /d "%SCRIPT_DIR%"
python train_finance_models.py
if errorlevel 1 (
    echo ERROR: Training failed. Check Python installation.
    pause
    exit /b 1
)

echo.
echo [3/3] Starting Finance AI service on port 8002...
uvicorn finance_ai_service:app --host 0.0.0.0 --port 8002 --reload

pause
