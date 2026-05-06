@echo off
echo ============================================
echo  AgriProtect Finance AI Service
echo ============================================

set SCRIPT_DIR=%~dp0
set VENV_PYTHON=%SCRIPT_DIR%venv\Scripts\python.exe
set MODELS_DIR=%SCRIPT_DIR%..\models

REM Si le venv n'existe pas, le creer et installer les dependances
if not exist "%VENV_PYTHON%" (
    echo [1/3] Creation de l'environnement virtuel...
    python -m venv "%SCRIPT_DIR%venv"
    echo [2/3] Installation des dependances...
    "%SCRIPT_DIR%venv\Scripts\pip.exe" install fastapi uvicorn scikit-learn joblib numpy xgboost
) else (
    echo [INFO] venv deja present - dependances OK
)

REM Entrainer les modeles si les .pkl n'existent pas encore
if not exist "%MODELS_DIR%\financial_risk_model.pkl" (
    echo [3/3] Entrainement des modeles ML...
    cd /d "%SCRIPT_DIR%"
    "%VENV_PYTHON%" train_finance_models.py
    if errorlevel 1 (
        echo ERREUR: Entrainement echoue.
        pause
        exit /b 1
    )
) else (
    echo [INFO] Modeles .pkl deja presents - pas besoin de reentrainer
)

echo.
echo Demarrage du service Finance AI sur le port 8002...
echo Laissez cette fenetre ouverte pendant l'utilisation de l'application.
echo.
cd /d "%SCRIPT_DIR%"
"%VENV_PYTHON%" -m uvicorn finance_ai_service:app --host 0.0.0.0 --port 8002

pause
