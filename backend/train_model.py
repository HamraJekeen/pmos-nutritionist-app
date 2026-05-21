import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.linear_model import LogisticRegression
import joblib

# Load the dataset
df = pd.read_csv('cleaned_pcos_data.csv')

# Handle any non-numeric values (like 'a' in AMH(ng/mL))
for col in df.columns:
    if df[col].dtype == object:
        df[col] = pd.to_numeric(df[col], errors='coerce')

# Fill missing values with column medians
df = df.fillna(df.median())

# Define Features and Target
features = [
    "Age (yrs)",
    "BMI",
    "FSH(mIU/mL)",
    "LH(mIU/mL)",
    "TSH (mIU/L)",
    "PRL(ng/mL)",
    "AMH(ng/mL)",
    "PRG(ng/mL)",
    "RBS(mg/dl)",
    "Weight gain(Y/N)",
    "hair growth(Y/N)",
    "Skin darkening (Y/N)",
    "Hair loss(Y/N)",
    "Pimples(Y/N)",
    "LH/FSH",
]

X = df[features]
y = df["PCOS (Y/N)"]

# ============================================================
# 8. SPLIT DATASET (70 / 15 / 15)
# ============================================================

X_train, X_temp, y_train, y_temp = train_test_split(
    X, y, test_size=0.30, random_state=42, stratify=y
)
X_val, X_test, y_val, y_test = train_test_split(
    X_temp, y_temp, test_size=0.50, random_state=42, stratify=y_temp
)

print(f"\nTrain: {X_train.shape} | Val: {X_val.shape} | Test: {X_test.shape}")

# Scale the features
scaler = StandardScaler()
X_train_scaled = scaler.fit_transform(X_train)
X_val_scaled = scaler.transform(X_val)
X_test_scaled = scaler.transform(X_test)

# Train Logistic Regression Model
lr_model = LogisticRegression(max_iter=1000, random_state=42)
lr_model.fit(X_train_scaled, y_train)

# Evaluate on Validation
val_acc = lr_model.score(X_val_scaled, y_val)
print(f"Validation Accuracy: {val_acc:.4f}")

# Evaluate on Test
test_acc = lr_model.score(X_test_scaled, y_test)
print(f"Test Accuracy: {test_acc:.4f}")

# Save the model, scaler, and features list
joblib.dump(lr_model, 'pcos_risk_profile_model.pkl')
joblib.dump(scaler, 'pcos_scaler.pkl')
joblib.dump(features, 'pcos_features.pkl')

print("\nModel training complete! Files saved:")
print("- pcos_risk_profile_model.pkl")
print("- pcos_scaler.pkl")
print("- pcos_features.pkl")
