"""
Train and save ML models for Financial Risk (accounting) and Savings Alert (savings).
Based on AgriProtect Finance Notebook - Module 2 & Module 3.
Run: python train_finance_models.py
Models are saved to ../models/
"""

import os
import numpy as np
import joblib
from sklearn.ensemble import RandomForestClassifier
from sklearn.neighbors import KNeighborsClassifier
from sklearn.tree import DecisionTreeClassifier
from sklearn.svm import SVC
from sklearn.preprocessing import StandardScaler, LabelEncoder
from sklearn.decomposition import PCA
from sklearn.model_selection import train_test_split

try:
    from xgboost import XGBClassifier
    HAS_XGB = True
except ImportError:
    HAS_XGB = False
    print("WARNING: xgboost not installed — pip install xgboost — falling back to RandomForest for Module 3")

np.random.seed(42)
N = 3000
OUTPUT_DIR = os.path.join(os.path.dirname(__file__), "..", "models")
os.makedirs(OUTPUT_DIR, exist_ok=True)

# ─────────────────────────────────────────────
# MODULE 2 — Financial Risk Classification
# Features: Revenue, Expenses, Debt_to_Equity, Net_Profit,
#           Avg_Temperature, Rainfall, Drought_Index, Flood_Risk_Score
# Target:   Financial_Risk_Level (Low / Medium / High)
# ─────────────────────────────────────────────
print("Training Module 2: Financial Risk Classifier...")

revenue        = np.random.uniform(3000, 60000, N)
expenses       = revenue * np.random.uniform(0.3, 1.1, N)
net_profit     = revenue - expenses
debt_to_equity = np.random.uniform(0.0, 3.5, N)
avg_temp       = np.random.uniform(18, 40, N)
rainfall       = np.random.uniform(100, 900, N)
drought_index  = np.random.uniform(0, 10, N)
flood_risk     = np.random.uniform(0, 10, N)

def classify_financial_risk(np_, dte, di, fr, rev):
    if np_ < 0 or dte > 2.5 or (di > 7.5 and fr > 7.0):
        return "High"
    elif np_ > rev * 0.25 and dte < 0.8 and di < 3.0 and fr < 3.0:
        return "Low"
    else:
        return "Medium"

risk_labels = [
    classify_financial_risk(net_profit[i], debt_to_equity[i], drought_index[i], flood_risk[i], revenue[i])
    for i in range(N)
]

X_risk = np.column_stack([revenue, expenses, debt_to_equity, net_profit,
                           avg_temp, rainfall, drought_index, flood_risk])
le_risk = LabelEncoder()
y_risk = le_risk.fit_transform(risk_labels)

X_train, X_test, y_train, y_test = train_test_split(X_risk, y_risk, test_size=0.2, random_state=42, stratify=y_risk)

scaler_risk = StandardScaler()
X_train_scaled = scaler_risk.fit_transform(X_train)
X_test_scaled  = scaler_risk.transform(X_test)

pca_risk = PCA(n_components=0.95, random_state=42)
X_train_pca = pca_risk.fit_transform(X_train_scaled)
X_test_pca  = pca_risk.transform(X_test_scaled)

# Notebook compares: KNN (n=7), DecisionTree (max_depth=10), RandomForest (n=300), SVM
risk_candidates = {
    "KNN":          KNeighborsClassifier(n_neighbors=7, n_jobs=-1),
    "DecisionTree": DecisionTreeClassifier(max_depth=10, random_state=42),
    "RandomForest": RandomForestClassifier(n_estimators=300, random_state=42, n_jobs=-1),
    "SVM":          SVC(probability=True, random_state=42),
}

best_risk_name, best_risk_model, best_risk_acc = None, None, 0.0
for name, clf in risk_candidates.items():
    clf.fit(X_train_pca, y_train)
    acc_ = clf.score(X_test_pca, y_test)
    print(f"  [{name}] accuracy: {acc_:.3f}")
    if acc_ > best_risk_acc:
        best_risk_acc, best_risk_name, best_risk_model = acc_, name, clf

print(f"  [OK] Best model: {best_risk_name} ({best_risk_acc:.3f})")
print(f"  Classes: {le_risk.classes_}")

joblib.dump(best_risk_model, os.path.join(OUTPUT_DIR, "financial_risk_model.pkl"))
joblib.dump(scaler_risk,     os.path.join(OUTPUT_DIR, "financial_risk_scaler.pkl"))
joblib.dump(pca_risk,        os.path.join(OUTPUT_DIR, "financial_risk_pca.pkl"))
joblib.dump(le_risk,         os.path.join(OUTPUT_DIR, "financial_risk_encoder.pkl"))
print("  Saved: financial_risk_model.pkl (best), scaler, pca, encoder")

# ─────────────────────────────────────────────
# MODULE 3 — Agricultural Savings Alert
# Notebook: XGBClassifier (primary), RandomForestClassifier, KNeighborsClassifier
# Features: Revenue, Expenses, Loan_Amount,
#           Drought_Index, Flood_Risk_Score, Policy_Support_Score
# Target:   AlertStatus (Insufficient / Moderate / Good)
# Derived via notebook formulas:
#   Recommended_Savings = 0.12*Rev - 0.05*Exp - 0.04*Loan - 10*DI - 8*FR + 2*PS
#   Saving_Capacity     = 0.30*max(Rev-Exp,0) - 0.08*Loan - 15*DI - 12*FR + 3*PS
#   ratio = Saving_Capacity / Recommended_Savings
#   AlertStatus: Insufficient(<0.70), Moderate([0.70,1.00)), Good(>=1.00)
# ─────────────────────────────────────────────
print("\nTraining Module 3: Agricultural Savings Alert Classifier...")

rev_s        = np.random.uniform(3000, 60000, N)
exp_s        = rev_s * np.random.uniform(0.3, 1.1, N)
loan_amount  = np.random.uniform(0, 25000, N)
drought_s    = np.random.uniform(0, 10, N)
flood_s      = np.random.uniform(0, 10, N)
policy_s     = np.random.uniform(1, 10, N)

rec_savings = (0.12 * rev_s - 0.05 * exp_s - 0.04 * loan_amount
               - 10.0 * drought_s - 8.0 * flood_s + 2.0 * policy_s)
rec_savings = np.maximum(rec_savings, 1.0)

saving_cap  = (0.30 * np.maximum(rev_s - exp_s, 0)
               - 0.08 * loan_amount - 15.0 * drought_s
               - 12.0 * flood_s + 3.0 * policy_s)

ratio = saving_cap / rec_savings

def classify_savings(r):
    if r < 0.70:
        return "Insufficient"
    elif r < 1.00:
        return "Moderate"
    else:
        return "Good"

savings_labels = [classify_savings(ratio[i]) for i in range(N)]

X_sav = np.column_stack([rev_s, exp_s, loan_amount, drought_s, flood_s, policy_s])
le_sav = LabelEncoder()
y_sav = le_sav.fit_transform(savings_labels)

X_tr, X_te, y_tr, y_te = train_test_split(X_sav, y_sav, test_size=0.2, random_state=42, stratify=y_sav)

scaler_sav = StandardScaler()
X_tr_scaled = scaler_sav.fit_transform(X_tr)
X_te_scaled = scaler_sav.transform(X_te)

pca_sav = PCA(n_components=0.95, random_state=42)
X_tr_pca = pca_sav.fit_transform(X_tr_scaled)
X_te_pca = pca_sav.transform(X_te_scaled)

# Train all 3 models from the notebook, select the best
candidates = {
    "RandomForest": RandomForestClassifier(n_estimators=300, max_depth=12, random_state=42, n_jobs=-1),
    "KNN":          KNeighborsClassifier(n_neighbors=7, n_jobs=-1),
}
if HAS_XGB:
    candidates["XGBoost"] = XGBClassifier(
        n_estimators=300, max_depth=6, learning_rate=0.1,
        eval_metric="mlogloss",
        random_state=42, n_jobs=-1
    )

best_name, best_model, best_acc = None, None, 0.0
for name, clf in candidates.items():
    clf.fit(X_tr_pca, y_tr)
    acc_ = clf.score(X_te_pca, y_te)
    print(f"  [{name}] accuracy: {acc_:.3f}")
    if acc_ > best_acc:
        best_acc, best_name, best_model = acc_, name, clf

print(f"  [OK] Best model: {best_name} ({best_acc:.3f})")
print(f"  Classes: {le_sav.classes_}")

joblib.dump(best_model, os.path.join(OUTPUT_DIR, "savings_alert_model.pkl"))
joblib.dump(scaler_sav, os.path.join(OUTPUT_DIR, "savings_alert_scaler.pkl"))
joblib.dump(pca_sav,    os.path.join(OUTPUT_DIR, "savings_alert_pca.pkl"))
joblib.dump(le_sav,     os.path.join(OUTPUT_DIR, "savings_alert_encoder.pkl"))
print("  Saved: savings_alert_model.pkl (best), scaler, pca, encoder")

print("\n[OK] All models trained and saved to ../models/")
print("   Now launch finance_ai_service.py to serve predictions.")
