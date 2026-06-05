import pandas as pd
import numpy as np
import joblib
import random
import itertools
import warnings
import pyodbc
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List, Optional, Dict, Any
from fastapi.middleware.cors import CORSMiddleware
import json
import os

warnings.filterwarnings("ignore")

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Load Models
MODEL_PATH = "pcos_risk_profile_model.pkl"
SCALER_PATH = "pcos_scaler.pkl"
FEATURES_PATH = "pcos_features.pkl"

if os.path.exists(MODEL_PATH) and os.path.exists(SCALER_PATH) and os.path.exists(FEATURES_PATH):
    lr_model = joblib.load(MODEL_PATH)
    scaler = joblib.load(SCALER_PATH)
    features = joblib.load(FEATURES_PATH)
else:
    print("Warning: Model files not found. ML predictions will fail until models are trained/provided.")
    lr_model = None
    scaler = None
    features = []

# SQL Connection
def get_sql_connection():
    conn_str = (
        "DRIVER={ODBC Driver 17 for SQL Server};"
        "SERVER=MSI\\SQLEXPRESS;"
        "DATABASE=pcos_diet;"
        "Trusted_Connection=yes;"
        "TrustServerCertificate=yes;"
    )
    return pyodbc.connect(conn_str)

def load_food_db_from_sql(conn) -> pd.DataFrame:
    query = """
        SELECT
            f.food_name,
            pt.type_name        AS pcos_type,
            c.category_name,
            mt.meal_name,
            fp.portion
        FROM foods f
        JOIN food_pcos_type_map fm  ON f.food_id = fm.food_id
        JOIN pcos_types pt          ON fm.type_id = pt.type_id
        LEFT JOIN food_category_map fcm ON f.food_id = fcm.food_id
        LEFT JOIN categories c          ON fcm.category_id = c.category_id
        LEFT JOIN food_meal_map fmm ON f.food_id = fmm.food_id
        LEFT JOIN meal_types mt     ON fmm.meal_id = mt.meal_id
        LEFT JOIN food_portions fp  ON f.food_id = fp.food_id
        WHERE mt.meal_name IS NOT NULL AND c.category_name IS NOT NULL
        ORDER BY f.food_id, pt.type_name, mt.meal_name
    """
    df = pd.read_sql(query, conn)
    return df.drop_duplicates()

def initialize_db(conn):
    try:
        cursor = conn.cursor()
        cursor.execute("""
            IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='patient_inputs' and xtype='U')
            CREATE TABLE patient_inputs (
                id INT IDENTITY(1,1) PRIMARY KEY,
                patient_id VARCHAR(50),
                input_data NVARCHAR(MAX),
                created_at DATETIME DEFAULT GETDATE()
            )
        """)
        cursor.execute("""
            IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='daily_tracking' and xtype='U')
            CREATE TABLE daily_tracking (
                id INT IDENTITY(1,1) PRIMARY KEY,
                patient_id VARCHAR(50),
                tracking_date DATE,
                water_glasses INT DEFAULT 0,
                workout_minutes INT DEFAULT 0,
                notes NVARCHAR(MAX),
                adherence_percentage INT DEFAULT 0
            )
        """)
        cursor.execute("""
            IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='meal_logs' and xtype='U')
            CREATE TABLE meal_logs (
                id INT IDENTITY(1,1) PRIMARY KEY,
                patient_id VARCHAR(50),
                tracking_date DATE,
                meal_type VARCHAR(50),
                food_logged NVARCHAR(MAX),
                status VARCHAR(20) DEFAULT 'Pending'
            )
        """)
        cursor.execute("""
            IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='questions' and xtype='U')
            CREATE TABLE questions (
                id INT IDENTITY(1,1) PRIMARY KEY,
                patient_id VARCHAR(50),
                patient_name NVARCHAR(255),
                question_text NVARCHAR(MAX),
                answer_text NVARCHAR(MAX),
                status VARCHAR(20) DEFAULT 'Pending',
                asked_at DATETIME DEFAULT GETDATE(),
                answered_at DATETIME
            )
        """)
        cursor.execute("""
            IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='diet_plan_history' and xtype='U')
            CREATE TABLE diet_plan_history (
                id INT IDENTITY(1,1) PRIMARY KEY,
                patient_id VARCHAR(50),
                input_id INT,
                plan_data NVARCHAR(MAX),
                notes NVARCHAR(MAX),
                created_at DATETIME DEFAULT GETDATE()
            )
        """)
        cursor.execute("""
            IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='nutritionist_registrations' and xtype='U')
            CREATE TABLE nutritionist_registrations (
                id INT IDENTITY(1,1) PRIMARY KEY,
                name NVARCHAR(255),
                email VARCHAR(255) UNIQUE,
                slmc_number VARCHAR(100),
                nic_number VARCHAR(100),
                status VARCHAR(50) DEFAULT 'Pending',
                created_at DATETIME DEFAULT GETDATE()
            )
        """)
        cursor.execute("""
            IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='patient_details_form' and xtype='U')
            CREATE TABLE patient_details_form (
                patient_id VARCHAR(50) PRIMARY KEY,
                breakfast NVARCHAR(MAX),
                lunch NVARCHAR(MAX),
                dinner NVARCHAR(MAX),
                irregular_periods VARCHAR(50),
                period_cycle_length VARCHAR(50),
                fast_food_freq VARCHAR(50),
                veg_fruit_freq VARCHAR(50),
                regular_exercise VARCHAR(50),
                exercise_frequency VARCHAR(100),
                stress_level INT,
                updated_at DATETIME DEFAULT GETDATE()
            )
        """)
        try:
            cursor.execute("ALTER TABLE patient_details_form DROP COLUMN work_hours")
            cursor.execute("ALTER TABLE patient_details_form ADD regular_exercise VARCHAR(50)")
            cursor.execute("ALTER TABLE patient_details_form ADD exercise_frequency VARCHAR(100)")
        except:
            pass
        conn.commit()
    except Exception as e:
        print("Error initializing DB:", e)

try:
    conn = get_sql_connection()
    initialize_db(conn)
    food_db = load_food_db_from_sql(conn)
    conn.close()
except Exception as e:
    print("Error loading food DB or initializing:", e)
    food_db = pd.DataFrame() # Fallback

# Pydantic Models for Request Body
class PatientInput(BaseModel):
    patient_id: str
    patient_name: str
    age: int
    weight_kg: float
    height_cm: float
    fsh: float
    lh: float
    tsh: float
    prl: float
    prg: float
    rbs: float
    amh: float
    weight_gain: int
    hair_growth: int
    skin_darkening: int
    hair_loss: int
    pimples: int
    irregular_periods: int
    selected_conditions: List[str]
    pcos_type: str
    allergies: List[str]

class NutritionistInput(BaseModel):
    patient_id: str
    physical_activities: str
    sleeping_hours: float
    water_intake_goal: int = 8
    notes: str
    plan: Optional[Dict[str, Dict[str, Dict[str, str]]]] = None

class DailyTrackingInput(BaseModel):
    patient_id: str
    tracking_date: str
    water_glasses: int
    workout_minutes: int
    notes: str

class MealLogInput(BaseModel):
    patient_id: str
    tracking_date: str
    meal_type: str
    food_logged: str
    status: str

class QuestionInput(BaseModel):
    patient_id: str
    patient_name: str
    question_text: str

class AnswerInput(BaseModel):
    question_id: int
    answer_text: str

class PatientDetailsFormInput(BaseModel):
    patient_id: str
    breakfast: str
    lunch: str
    dinner: str
    irregular_periods: str
    period_cycle_length: str
    fast_food_freq: str
    veg_fruit_freq: str
    regular_exercise: str
    exercise_frequency: str
    stress_level: int

class HistoricalPlanResponse(BaseModel):
    id: int
    patient_id: str
    patient_name: str
    created_at: str
    plan_data: Dict[str, Dict[str, Dict[str, str]]]
    notes: str
    patient_inputs: Dict[str, Any]

class NutritionistRegistrationInput(BaseModel):
    name: str
    email: str
    slmc_number: str
    nic_number: str

class NutritionistRegistrationResponse(BaseModel):
    id: int
    name: str
    email: str
    slmc_number: str
    nic_number: str
    status: str
    created_at: str

class AdminVerifyInput(BaseModel):
    registration_id: int

# Condition mapping
condition_to_categories = {
    "hormonal_imbalance" : ["High-Fiber Foods","Low-GI Foods","Healthy Fats","Omega-3 Rich Foods","Vegetables","Lean Protein", "Herbal Teas"],
    "insulin_resistance" : ["Low-GI Foods","High-Fiber Foods","Lean Protein","Protein","Vegetables","Healthy Fats","Fermented Foods", "Herbal Teas"],
    "obesity"            : ["Vegetables","Fruits","Lean Protein","High-Fiber Foods","Low-GI Foods","Herbal Teas"],
    "thyroid"            : ["Protein","Lean Protein","Omega-3 Rich Foods","Vegetables","Fruits","Grains","Herbal Teas","Dairy"],
    "hypertension"       : ["Vegetables","Fruits","High-Fiber Foods","Lean Protein","Herbal Teas","Low-GI Foods"],
    "androgen_excess"    : ["Anti-Inflammatory Spices","Omega-3 Rich Foods","Vegetables","Fruits","Healthy Fats","High-Fiber Foods","Beverages","Fermented Foods","Herbal Teas", "Beverages",],
    "prolactin_imbalance": ["Vegetables","Fruits","High-Fiber Foods","Healthy Fats","Herbal Teas","Anti-Inflammatory Spices"],
    "low_progesterone"   : ["Healthy Fats","Omega-3 Rich Foods","Lean Protein","Protein","High-Fiber Foods","Grains","Beverages"],
    "high_amh"           : ["Low-GI Foods","High-Fiber Foods","Healthy Fats","Omega-3 Rich Foods","Vegetables","Lean Protein"],
    "strong_pcos_pattern": ["Low-GI Foods","High-Fiber Foods","Omega-3 Rich Foods","Anti-Inflammatory Spices","Vegetables","Lean Protein","Herbal Teas"],
    "high_pcos_risk"     : ["Low-GI Foods","High-Fiber Foods","Vegetables","Lean Protein"],
    "moderate_pcos_risk" : ["Low-GI Foods","High-Fiber Foods"],
    "low_pcos_risk"      : ["Vegetables","Fruits","Grains","Dairy"],
}

# --- Prediction Logic Ported from Snippet ---

def predict_pcos_risk_profile(p: dict) -> tuple:
    if lr_model is None:
        return 0.8, "High" # Dummy if no model

    if "LH/FSH" not in p:
        p["LH/FSH"] = p["LH(mIU/mL)"] / p["FSH(mIU/mL)"] if p["FSH(mIU/mL)"] != 0 else 0.0
    if "BMI" not in p:
        h = p["Height(Cm)"] / 100
        p["BMI"] = p["Weight (Kg)"] / (h ** 2) if h != 0 else 0.0

    input_df = pd.DataFrame([p], columns=features)
    input_df = input_df.fillna(0.0) # Handle missing features (like AMH) from frontend
    input_scaled = scaler.transform(input_df)
    probability = lr_model.predict_proba(input_scaled)[0][1]

    if probability >= 0.75: risk_level = "High"
    elif probability >= 0.50: risk_level = "Moderate"
    else: risk_level = "Low"

    return float(probability), risk_level

def detect_conditions(p: dict) -> list:
    conditions = set()
    
    if "BMI" not in p:
        h = p["Height(Cm)"] / 100
        p["BMI"] = p["Weight (Kg)"] / (h ** 2) if h != 0 else 0.0
    if "LH/FSH" not in p:
        p["LH/FSH"] = p["LH(mIU/mL)"] / p["FSH(mIU/mL)"] if p["FSH(mIU/mL)"] != 0 else 0.0

    if p.get("LH/FSH", 0) > 2: conditions.add("hormonal_imbalance")
    if p.get("RBS(mg/dl)", 0) >= 140: conditions.add("insulin_resistance")
    if p.get("TSH (mIU/L)", 0) > 5: conditions.add("thyroid")
    if p.get("PRL(ng/mL)", 0) > 20: conditions.add("prolactin_imbalance")
    if p.get("PRG(ng/mL)", 0) < 5: conditions.add("low_progesterone")
    if p.get("BMI", 0) > 25: conditions.add("obesity")
    if p.get("Weight gain(Y/N)", 0) == 1: conditions.add("obesity")
    if p.get("hair growth(Y/N)", 0) == 1 or p.get("Pimples(Y/N)", 0) == 1: conditions.add("androgen_excess")
    if p.get("Hair loss(Y/N)", 0) == 1: conditions.add("hormonal_imbalance")
    if p.get("Irregular periods(Y/N)", 0) == 1:
        conditions.add("hormonal_imbalance")
        conditions.add("low_progesterone")
        
    # ── Elevated AMH ─────────────────────────────
    if p.get("AMH(ng/mL)", 0) > 4.5:
        conditions.add("high_amh")

    # ── Strong PCOS hormonal pattern ────────────
    if (
        p.get("AMH(ng/mL)", 0) > 4.5 and
        p.get("LH/FSH", 0) > 2
    ):
        conditions.add("strong_pcos_pattern")
        
    if (
        p.get("AMH(ng/mL)", 0) > 4.5 and
        (
            p.get("hair growth(Y/N)", 0) == 1 or
            p.get("Pimples(Y/N)", 0) == 1 or
            p.get("Irregular periods(Y/N)", 0) == 1
        )
    ):
        conditions.add("strong_pcos_pattern")

    selected = p.get("selected_conditions", [])
    if "diabetes" in selected: conditions.add("insulin_resistance")
    if "thyroid" in selected: conditions.add("thyroid")
    if "hypertension" in selected: conditions.add("hypertension")

    return list(conditions)

def filter_allergies(food_df, allergies):
    if not allergies: return food_df
    allergy_keywords = [a.strip().lower() for a in allergies if a.strip()]
    if not allergy_keywords: return food_df

    def is_safe(food_name):
        name_lower = food_name.lower()
        for keyword in allergy_keywords:
            if keyword in name_lower: return False
        return True

    return food_df[food_df["food_name"].apply(is_safe)].copy()

def filter_foods(food_df, pcos_type, final_categories, allergies=None):
    filtered = food_df[
        (food_df["pcos_type"] == pcos_type) &
        (food_df["category_name"].isin(final_categories))
    ].copy()
    
    if allergies: filtered = filter_allergies(filtered, allergies)
    filtered = filtered.drop_duplicates(subset=["food_name", "meal_name"])

    if filtered.empty:
        filtered = food_df[
            (food_df["pcos_type"] == "General") &
            (food_df["category_name"].isin(final_categories))
        ].copy()
        if allergies: filtered = filter_allergies(filtered, allergies)
        filtered = filtered.drop_duplicates(subset=["food_name", "meal_name"])
    return filtered

def generate_7_day_plan(food_df):
    days = ["Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"]
    meals = ["Breakfast","Lunch","Snack","Dinner","Beverage"]
    meal_pools = {}
    
    for meal in meals:
        meal_foods = food_df[food_df["meal_name"] == meal].sample(frac=1, random_state=random.randint(0, 9999)).reset_index(drop=True)
        meal_pools[meal] = itertools.cycle(meal_foods.iterrows()) if len(meal_foods) > 0 else None

    plan = {}
    for day in days:
        plan[day] = {}
        for meal in meals:
            if meal_pools[meal] is None:
                plan[day][meal] = {"food": "Nutritionist review required", "portion": "-"}
            else:
                num_items = random.randint(2, 3) if meal in ["Breakfast", "Lunch", "Dinner"] else 1
                foods = []
                portions = []
                for _ in range(num_items):
                    _, row = next(meal_pools[meal])
                    if row["food_name"] not in foods:
                        foods.append(row["food_name"])
                        portions.append(str(row["portion"]))
                plan[day][meal] = {"food": " + ".join(foods), "portion": " + ".join(portions)}
    return plan

def save_diet_plan_to_sql(meal_plan, patient_id, pcos_type, risk_level):
    insert_sql = """
        INSERT INTO diet_plans
            (patient_id, pcos_type, risk_level, day_name, meal_type, food_name, portion, status)
        VALUES (?, ?, ?, ?, ?, ?, ?, 'Pending Review')
    """
    try:
        conn = get_sql_connection()
        cursor = conn.cursor()
        
        # Delete all old plans if generating new
        cursor.execute("DELETE FROM diet_plans WHERE patient_id = ?", patient_id)

        for day, meals in meal_plan.items():
            for meal_type, details in meals.items():
                cursor.execute(insert_sql, patient_id, pcos_type, risk_level, day, meal_type, details["food"], details["portion"])
        conn.commit()
    except Exception as e:
        print("SQL Error:", e)
    finally:
        if 'conn' in locals() and conn: conn.close()

category_benefits = {
    "Low-GI Foods": "helps stabilize blood sugar levels and manage insulin resistance.",
    "High-Fiber Foods": "aids digestion and helps maintain steady blood sugar levels.",
    "Healthy Fats": "supports hormone production and overall endocrine health.",
    "Omega-3 Rich Foods": "reduces inflammation and supports cardiovascular health.",
    "Vegetables": "provides essential vitamins, minerals, and antioxidants for hormone balance.",
    "Lean Protein": "supports muscle maintenance and helps with satiety and blood sugar control.",
    "Protein": "essential for tissue repair and hormone synthesis.",
    "Herbal Teas": "helps reduce stress and supports hormonal detox.",
    "Fruits": "provides natural antioxidants and essential micronutrients.",
    "Fermented Foods": "supports gut health, which is crucial for hormone metabolism.",
    "Grains": "provides sustained energy and essential B-vitamins.",
    "Anti-Inflammatory Spices": "helps reduce systemic inflammation associated with PCOS.",
    "Dairy": "provides calcium and vitamin D for bone health (if tolerated).",
    "Beverages": "supports hydration and metabolic processes."
}

def generate_explanation(final_conditions: list,
                         final_categories : list,
                         plan: dict,
                         food_df: pd.DataFrame,
                         allergies        : list = None) -> list:
    explanations = []

    used_foods = set()
    for day, meals in plan.items():
        for meal, details in meals.items():
            for f in details["food"].split(" + "):
                f_clean = f.strip()
                if f_clean and f_clean != "Nutritionist review required" and f_clean != "-":
                    used_foods.add(f_clean)

    # Explanation for each unique food item
    for f in sorted(list(used_foods)):
        f_info = food_df[food_df["food_name"] == f]
        if not f_info.empty:
            cat = f_info.iloc[0]["category_name"]
            benefit = category_benefits.get(cat, "supports your overall PCOS nutrition plan.")
            explanations.append(f"{f} ({cat}): This {benefit}")

    if allergies:
        explanations.append(
            f"Allergy filter applied — excluded foods containing: "
            f"{', '.join(allergies)}."
        )

    return explanations

# API Endpoints
@app.post("/api/predict_diet")
def predict_diet(input_data: PatientInput):
    # Convert input to ML dict
    p = {
        "Age (yrs)": input_data.age,
        "Weight (Kg)": input_data.weight_kg,
        "Height(Cm)": input_data.height_cm,
        "FSH(mIU/mL)": input_data.fsh,
        "LH(mIU/mL)": input_data.lh,
        "TSH (mIU/L)": input_data.tsh,
        "PRL(ng/mL)": input_data.prl,
        "PRG(ng/mL)": input_data.prg,
        "RBS(mg/dl)": input_data.rbs,
        "AMH(ng/mL)": input_data.amh,
        "Weight gain(Y/N)": input_data.weight_gain,
        "hair growth(Y/N)": input_data.hair_growth,
        "Skin darkening (Y/N)": input_data.skin_darkening,
        "Hair loss(Y/N)": input_data.hair_loss,
        "Pimples(Y/N)": input_data.pimples,
        "Irregular periods(Y/N)": input_data.irregular_periods,
        "selected_conditions": input_data.selected_conditions
    }

    # Pipeline
    prob, risk = predict_pcos_risk_profile(p)
    conds = detect_conditions(p)
    
    final_conds = set(conds)
    if risk == "High": final_conds.add("high_pcos_risk")
    elif risk == "Moderate": final_conds.add("moderate_pcos_risk")
    else: final_conds.add("low_pcos_risk")

    final_cats = set()
    for c in final_conds:
        final_cats.update(condition_to_categories.get(c, []))

    pcos_type = input_data.pcos_type if input_data.pcos_type else "General"
    
    filtered_foods = filter_foods(food_db, pcos_type, list(final_cats), input_data.allergies)
    
    plan = generate_7_day_plan(filtered_foods)

    save_diet_plan_to_sql(plan, input_data.patient_id, pcos_type, risk)

    explanation = generate_explanation(list(final_conds), list(final_cats), plan, filtered_foods, input_data.allergies)

    try:
        conn = get_sql_connection()
        cursor = conn.cursor()
        data_to_save = input_data.dict()
        data_to_save["explanation"] = explanation
        cursor.execute("INSERT INTO patient_inputs (patient_id, input_data) VALUES (?, ?)", input_data.patient_id, json.dumps(data_to_save))
        conn.commit()
        conn.close()
    except Exception as e:
        print("SQL Input Save Error:", e)

    return {"message": "Diet plan generated successfully", "risk_level": risk, "plan": plan}

@app.get("/api/plans/{patient_id}")
def get_patient_plan(patient_id: str):
    try:
        conn = get_sql_connection()
        query = "SELECT day_name, meal_type, food_name, portion, status, nutritionist_notes FROM diet_plans WHERE patient_id = ?"
        df = pd.read_sql(query, conn, params=[patient_id])
        conn.close()
        
        if df.empty:
            return {"status": "No Plan Found", "plan": {}}
            
        status = df.iloc[0]['status']
        nutritionist_notes = df.iloc[0]['nutritionist_notes'] if 'nutritionist_notes' in df.columns else None
        plan = {}
        for _, row in df.iterrows():
            d = row['day_name']
            if d not in plan: plan[d] = {}
            plan[d][row['meal_type']] = {"food": row['food_name'], "portion": row['portion']}
            
        return {"status": status, "plan": plan, "nutritionist_notes": nutritionist_notes}
    except Exception as e:
        return HTTPException(status_code=500, detail=str(e))

@app.get("/api/nutritionist/patients")
def get_all_patients():
    try:
        conn = get_sql_connection()
        cursor = conn.cursor()
        cursor.execute("SELECT patient_id, MAX(status) FROM diet_plans GROUP BY patient_id")
        rows = cursor.fetchall()
        
        patient_list = []
        for r in rows:
            p_id = r[0]
            status = r[1]
            cursor.execute("SELECT TOP 1 input_data FROM patient_inputs WHERE patient_id = ? ORDER BY created_at DESC", p_id)
            input_row = cursor.fetchone()
            p_name = "Unknown"
            if input_row:
                try:
                    data = json.loads(input_row[0])
                    p_name = data.get("patient_name", "Unknown")
                except:
                    pass
            patient_list.append({"patient_id": p_id, "patient_name": p_name, "status": status})
            
        conn.close()
        return patient_list
    except Exception as e:
        return HTTPException(status_code=500, detail=str(e))

@app.get("/api/patient/{patient_id}/inputs")
def get_patient_inputs(patient_id: str):
    try:
        conn = get_sql_connection()
        cursor = conn.cursor()
        cursor.execute("SELECT TOP 1 input_data FROM patient_inputs WHERE patient_id = ? ORDER BY created_at DESC", patient_id)
        row = cursor.fetchone()
        conn.close()
        if row:
            return json.loads(row[0])
        return {"error": "No inputs found"}
    except Exception as e:
        return HTTPException(status_code=500, detail=str(e))

@app.post("/api/nutritionist/approve")
def approve_plan(input_data: NutritionistInput):
    # Nutritionist approves and adds activities/sleep
    try:
        conn = get_sql_connection()
        cursor = conn.cursor()

        combined_notes = json.dumps({
            "activities": input_data.physical_activities,
            "sleep": input_data.sleeping_hours,
            "water": input_data.water_intake_goal,
            "notes": input_data.notes
        })
        
        if input_data.plan:
            # Fetch existing risk and type
            cursor.execute("SELECT TOP 1 pcos_type, risk_level FROM diet_plans WHERE patient_id = ? AND status = 'Pending Review'", input_data.patient_id)
            row = cursor.fetchone()
            p_type = row[0] if row else "General"
            r_level = row[1] if row else "Unknown"

            cursor.execute("DELETE FROM diet_plans WHERE patient_id = ? AND status = 'Pending Review'", input_data.patient_id)
            insert_sql = """
                INSERT INTO diet_plans
                    (patient_id, pcos_type, risk_level, day_name, meal_type, food_name, portion, status, nutritionist_notes)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'Approved', ?)
            """
            for day, meals in input_data.plan.items():
                for meal_type, details in meals.items():
                    cursor.execute(insert_sql, input_data.patient_id, p_type, r_level, day, meal_type, details["food"], details["portion"], combined_notes)
        else:
            cursor.execute("UPDATE diet_plans SET status = 'Approved', nutritionist_notes = ? WHERE patient_id = ? AND status = 'Pending Review'", 
                           combined_notes, input_data.patient_id)
            
        # --- Save Snapshot to diet_plan_history ---
        cursor.execute("SELECT TOP 1 id FROM patient_inputs WHERE patient_id = ? ORDER BY created_at DESC", input_data.patient_id)
        input_row = cursor.fetchone()
        input_id = input_row[0] if input_row else None

        plan_snapshot = input_data.plan
        if not plan_snapshot:
            # Reconstruct from DB if no tweaks were sent
            cursor.execute("SELECT day_name, meal_type, food_name, portion FROM diet_plans WHERE patient_id = ? AND status = 'Approved'", input_data.patient_id)
            plan_snapshot = {}
            for r in cursor.fetchall():
                d, m, f, p = r
                if d not in plan_snapshot: plan_snapshot[d] = {}
                plan_snapshot[d][m] = {"food": f, "portion": p}

        cursor.execute("""
            INSERT INTO diet_plan_history (patient_id, input_id, plan_data, notes)
            VALUES (?, ?, ?, ?)
        """, input_data.patient_id, input_id, json.dumps(plan_snapshot), combined_notes)
        # ----------------------------------------
        
        conn.commit()
        conn.close()
        return {"message": "Plan approved and details updated."}
    except Exception as e:
        return HTTPException(status_code=500, detail=str(e))

@app.get("/api/nutritionist/plans/history", response_model=List[HistoricalPlanResponse])
def get_plan_history():
    try:
        conn = get_sql_connection()
        cursor = conn.cursor()
        
        cursor.execute("""
            SELECT h.id, h.patient_id, h.plan_data, h.notes, h.created_at, p.input_data 
            FROM diet_plan_history h
            LEFT JOIN patient_inputs p ON h.input_id = p.id
            ORDER BY h.created_at DESC
        """)
        
        rows = cursor.fetchall()
        
        # Build a fallback mapping for patient names
        cursor.execute("SELECT patient_id, input_data FROM patient_inputs ORDER BY created_at ASC")
        fallback_names = {}
        for r in cursor.fetchall():
            try:
                data = json.loads(r[1])
                if data.get("patient_name"):
                    fallback_names[r[0]] = data.get("patient_name")
            except: pass

        history = []
        for row in rows:
            try:
                inputs = json.loads(row[5]) if row[5] else {}
                p_name = inputs.get("patient_name")
                if not p_name:
                    p_name = fallback_names.get(row[1], "Unknown")
                
                history.append(HistoricalPlanResponse(
                    id=row[0],
                    patient_id=row[1],
                    patient_name=p_name,
                    created_at=row[4].strftime("%Y-%m-%d %H:%M:%S") if row[4] else "",
                    plan_data=json.loads(row[2]) if row[2] else {},
                    notes=row[3] or "",
                    patient_inputs=inputs
                ))
            except Exception as e:
                print("Error parsing history row:", e)
                pass
                
        conn.close()
        return history
    except Exception as e:
        return HTTPException(status_code=500, detail=str(e))

@app.post("/api/patient/track/daily")
def update_daily_tracking(input_data: DailyTrackingInput):
    try:
        conn = get_sql_connection()
        cursor = conn.cursor()
        
        # Check if exists
        cursor.execute("SELECT id FROM daily_tracking WHERE patient_id = ? AND tracking_date = ?", input_data.patient_id, input_data.tracking_date)
        row = cursor.fetchone()
        
        if row:
            cursor.execute("""
                UPDATE daily_tracking 
                SET water_glasses = ?, workout_minutes = ?, notes = ? 
                WHERE patient_id = ? AND tracking_date = ?
            """, input_data.water_glasses, input_data.workout_minutes, input_data.notes, input_data.patient_id, input_data.tracking_date)
        else:
            cursor.execute("""
                INSERT INTO daily_tracking (patient_id, tracking_date, water_glasses, workout_minutes, notes)
                VALUES (?, ?, ?, ?, ?)
            """, input_data.patient_id, input_data.tracking_date, input_data.water_glasses, input_data.workout_minutes, input_data.notes)
            
        conn.commit()
        conn.close()
        return {"message": "Daily tracking updated successfully"}
    except Exception as e:
        return HTTPException(status_code=500, detail=str(e))

@app.post("/api/patient/track/meal")
def log_meal(input_data: MealLogInput):
    try:
        conn = get_sql_connection()
        cursor = conn.cursor()
        
        cursor.execute("SELECT id FROM meal_logs WHERE patient_id = ? AND tracking_date = ? AND meal_type = ?", 
                       input_data.patient_id, input_data.tracking_date, input_data.meal_type)
        row = cursor.fetchone()
        
        if row:
            cursor.execute("""
                UPDATE meal_logs 
                SET food_logged = ?, status = ? 
                WHERE patient_id = ? AND tracking_date = ? AND meal_type = ?
            """, input_data.food_logged, input_data.status, input_data.patient_id, input_data.tracking_date, input_data.meal_type)
        else:
            cursor.execute("""
                INSERT INTO meal_logs (patient_id, tracking_date, meal_type, food_logged, status)
                VALUES (?, ?, ?, ?, ?)
            """, input_data.patient_id, input_data.tracking_date, input_data.meal_type, input_data.food_logged, input_data.status)
            
        # Update overall adherence for the day (simple calculation based on meals logged vs 5 total meals)
        cursor.execute("SELECT status FROM meal_logs WHERE patient_id = ? AND tracking_date = ?", input_data.patient_id, input_data.tracking_date)
        meals = cursor.fetchall()
        total_daily_meals = 5 # Breakfast, Lunch, Snack, Dinner, Beverage
        logged = sum(1 for m in meals if m[0] == 'Logged' or m[0] == 'Completed')
        adherence = int((logged / total_daily_meals) * 100)
        cursor.execute("UPDATE daily_tracking SET adherence_percentage = ? WHERE patient_id = ? AND tracking_date = ?", adherence, input_data.patient_id, input_data.tracking_date)
        if cursor.rowcount == 0:
            cursor.execute("INSERT INTO daily_tracking (patient_id, tracking_date, adherence_percentage) VALUES (?, ?, ?)", input_data.patient_id, input_data.tracking_date, adherence)
                
        conn.commit()
        conn.close()
        return {"message": "Meal logged successfully"}
    except Exception as e:
        return HTTPException(status_code=500, detail=str(e))

@app.get("/api/patient/track/{patient_id}")
def get_patient_tracking(patient_id: str, date: str):
    try:
        conn = get_sql_connection()
        cursor = conn.cursor()
        
        cursor.execute("SELECT water_glasses, workout_minutes, notes, adherence_percentage FROM daily_tracking WHERE patient_id = ? AND tracking_date = ?", patient_id, date)
        daily_row = cursor.fetchone()
        
        cursor.execute("SELECT meal_type, food_logged, status FROM meal_logs WHERE patient_id = ? AND tracking_date = ?", patient_id, date)
        meal_rows = cursor.fetchall()
        
        # Get weekly history
        cursor.execute("SELECT tracking_date, adherence_percentage FROM daily_tracking WHERE patient_id = ? AND tracking_date >= DATEADD(day, -7, ?) ORDER BY tracking_date", patient_id, date)
        weekly_rows = cursor.fetchall()
        
        conn.close()
        
        daily_data = {
            "water_glasses": daily_row[0] if daily_row else 0,
            "workout_minutes": daily_row[1] if daily_row else 0,
            "notes": daily_row[2] if daily_row else "",
            "adherence_percentage": daily_row[3] if daily_row else 0
        }
        
        meals_data = [{"meal_type": r[0], "food_logged": r[1], "status": r[2]} for r in meal_rows]
        weekly_data = [{"date": r[0].strftime("%Y-%m-%d"), "adherence": r[1]} for r in weekly_rows]
        
        return {
            "daily": daily_data,
            "meals": meals_data,
            "weekly": weekly_data
        }
    except Exception as e:
        return HTTPException(status_code=500, detail=str(e))

@app.post("/api/patient/qa")
def ask_question(input_data: QuestionInput):
    try:
        conn = get_sql_connection()
        cursor = conn.cursor()
        cursor.execute("""
            INSERT INTO questions (patient_id, patient_name, question_text)
            VALUES (?, ?, ?)
        """, input_data.patient_id, input_data.patient_name, input_data.question_text)
        conn.commit()
        conn.close()
        return {"message": "Question submitted"}
    except Exception as e:
        return HTTPException(status_code=500, detail=str(e))

@app.get("/api/patient/qa/{patient_id}")
def get_patient_questions(patient_id: str):
    try:
        conn = get_sql_connection()
        cursor = conn.cursor()
        cursor.execute("SELECT id, question_text, answer_text, status, asked_at, answered_at FROM questions WHERE patient_id = ? ORDER BY asked_at DESC", patient_id)
        rows = cursor.fetchall()
        conn.close()
        
        questions = []
        for r in rows:
            questions.append({
                "id": r[0],
                "question_text": r[1],
                "answer_text": r[2],
                "status": r[3],
                "asked_at": r[4].strftime("%Y-%m-%d %H:%M") if r[4] else None,
                "answered_at": r[5].strftime("%Y-%m-%d %H:%M") if r[5] else None
            })
        return questions
    except Exception as e:
        return HTTPException(status_code=500, detail=str(e))

@app.get("/api/nutritionist/tracking")
def get_nutritionist_tracking():
    try:
        conn = get_sql_connection()
        cursor = conn.cursor()
        # Getting the latest tracking entry for each patient
        query = """
            SELECT d.patient_id, i.input_data, d.adherence_percentage 
            FROM daily_tracking d
            INNER JOIN (
                SELECT patient_id, MAX(tracking_date) as max_date 
                FROM daily_tracking GROUP BY patient_id
            ) md ON d.patient_id = md.patient_id AND d.tracking_date = md.max_date
            LEFT JOIN (
                SELECT patient_id, input_data,
                ROW_NUMBER() OVER(PARTITION BY patient_id ORDER BY created_at DESC) as rn
                FROM patient_inputs
            ) i ON d.patient_id = i.patient_id AND i.rn = 1
        """
        cursor.execute(query)
        rows = cursor.fetchall()
        conn.close()
        
        tracking_list = []
        for r in rows:
            p_name = "Unknown"
            if r[1]:
                try:
                    data = json.loads(r[1])
                    p_name = data.get("patient_name", "Unknown")
                except:
                    pass
            tracking_list.append({
                "patient_id": r[0],
                "patient_name": p_name,
                "adherence": r[2]
            })
        return tracking_list
    except Exception as e:
        return HTTPException(status_code=500, detail=str(e))

@app.get("/api/nutritionist/qa")
def get_nutritionist_qa():
    try:
        conn = get_sql_connection()
        cursor = conn.cursor()
        cursor.execute("SELECT id, patient_id, patient_name, question_text, answer_text, status, asked_at FROM questions ORDER BY asked_at DESC")
        rows = cursor.fetchall()
        conn.close()
        
        questions = []
        for r in rows:
            questions.append({
                "id": r[0],
                "patient_id": r[1],
                "patient_name": r[2],
                "question_text": r[3],
                "answer_text": r[4],
                "status": r[5],
                "asked_at": r[6].strftime("%Y-%m-%d %H:%M") if r[6] else None
            })
        return questions
    except Exception as e:
        return HTTPException(status_code=500, detail=str(e))

@app.post("/api/nutritionist/qa/answer")
def answer_question(input_data: AnswerInput):
    try:
        conn = get_sql_connection()
        cursor = conn.cursor()
        cursor.execute("""
            UPDATE questions 
            SET answer_text = ?, status = 'Answered', answered_at = GETDATE()
            WHERE id = ?
        """, input_data.answer_text, input_data.question_id)
        conn.commit()
        conn.close()
        return {"message": "Answer submitted"}
    except Exception as e:
        return HTTPException(status_code=500, detail=str(e))

@app.post("/api/nutritionist/register")
def register_nutritionist(input_data: NutritionistRegistrationInput):
    try:
        conn = get_sql_connection()
        cursor = conn.cursor()
        
        # Check if email already exists
        cursor.execute("SELECT id FROM nutritionist_registrations WHERE email = ?", input_data.email)
        if cursor.fetchone():
            raise HTTPException(status_code=400, detail="Email already registered")
            
        cursor.execute("""
            INSERT INTO nutritionist_registrations (name, email, slmc_number, nic_number)
            VALUES (?, ?, ?, ?)
        """, input_data.name, input_data.email, input_data.slmc_number, input_data.nic_number)
        
        conn.commit()
        conn.close()
        return {"message": "Registration submitted successfully. Pending admin approval."}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/admin/nutritionists/pending", response_model=List[NutritionistRegistrationResponse])
def get_pending_nutritionists():
    try:
        conn = get_sql_connection()
        cursor = conn.cursor()
        cursor.execute("""
            SELECT id, name, email, slmc_number, nic_number, status, created_at
            FROM nutritionist_registrations
            WHERE status = 'Pending'
            ORDER BY created_at ASC
        """)
        rows = cursor.fetchall()
        conn.close()
        
        results = []
        for r in rows:
            results.append(NutritionistRegistrationResponse(
                id=r[0],
                name=r[1],
                email=r[2],
                slmc_number=r[3],
                nic_number=r[4],
                status=r[5],
                created_at=r[6].strftime("%Y-%m-%d %H:%M:%S") if r[6] else ""
            ))
        return results
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/admin/nutritionists/verified", response_model=List[NutritionistRegistrationResponse])
def get_verified_nutritionists():
    try:
        conn = get_sql_connection()
        cursor = conn.cursor()
        cursor.execute("""
            SELECT id, name, email, slmc_number, nic_number, status, created_at
            FROM nutritionist_registrations
            WHERE status = 'Verified'
            ORDER BY created_at DESC
        """)
        rows = cursor.fetchall()
        conn.close()
        
        results = []
        for r in rows:
            results.append(NutritionistRegistrationResponse(
                id=r[0],
                name=r[1],
                email=r[2],
                slmc_number=r[3],
                nic_number=r[4],
                status=r[5],
                created_at=r[6].strftime("%Y-%m-%d %H:%M:%S") if r[6] else ""
            ))
        return results
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/admin/nutritionists/verify")
def verify_nutritionist(input_data: AdminVerifyInput):
    try:
        conn = get_sql_connection()
        cursor = conn.cursor()
        cursor.execute("""
            UPDATE nutritionist_registrations
            SET status = 'Verified'
            WHERE id = ?
        """, input_data.registration_id)
        if cursor.rowcount == 0:
            raise HTTPException(status_code=404, detail="Registration not found")
            
        conn.commit()
        conn.close()
        return {"message": "Nutritionist verified successfully"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.delete("/api/admin/nutritionists/{registration_id}")
def delete_nutritionist(registration_id: int):
    try:
        conn = get_sql_connection()
        cursor = conn.cursor()
        cursor.execute("""
            DELETE FROM nutritionist_registrations
            WHERE id = ?
        """, registration_id)
        
        if cursor.rowcount == 0:
            raise HTTPException(status_code=404, detail="Nutritionist profile not found")
            
        conn.commit()
        conn.close()
        return {"message": "Nutritionist profile deleted successfully"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/user/role")
def get_user_role(email: str):
    email = email.lower().strip()
    if email == "hamrajekeen@gmail.com":
        return {"role": "Admin"}
        
    try:
        conn = get_sql_connection()
        cursor = conn.cursor()
        cursor.execute("SELECT status FROM nutritionist_registrations WHERE email = ?", email)
        row = cursor.fetchone()
        conn.close()
        
        if row and row[0] == 'Verified':
            return {"role": "Nutritionist"}
        elif email == "nutritionisthamra@gmail.com":
            return {"role": "Nutritionist"} # Legacy fallback
            
        return {"role": "Patient"}
    except Exception as e:
        # Fallback in case of DB error
        if email == "nutritionisthamra@gmail.com":
            return {"role": "Nutritionist"}
        return {"role": "Patient"}

@app.post("/api/patient/details_form")
def save_patient_details_form(input_data: PatientDetailsFormInput):
    try:
        conn = get_sql_connection()
        cursor = conn.cursor()
        
        # Check if exists
        cursor.execute("SELECT patient_id FROM patient_details_form WHERE patient_id = ?", input_data.patient_id)
        if cursor.fetchone():
            cursor.execute("""
                UPDATE patient_details_form 
                SET breakfast = ?, lunch = ?, dinner = ?, irregular_periods = ?,
                    period_cycle_length = ?, fast_food_freq = ?, veg_fruit_freq = ?,
                    regular_exercise = ?, exercise_frequency = ?, stress_level = ?, updated_at = GETDATE()
                WHERE patient_id = ?
            """, input_data.breakfast, input_data.lunch, input_data.dinner, input_data.irregular_periods,
                 input_data.period_cycle_length, input_data.fast_food_freq, input_data.veg_fruit_freq,
                 input_data.regular_exercise, input_data.exercise_frequency, input_data.stress_level, input_data.patient_id)
        else:
            cursor.execute("""
                INSERT INTO patient_details_form 
                (patient_id, breakfast, lunch, dinner, irregular_periods, period_cycle_length, fast_food_freq, veg_fruit_freq, regular_exercise, exercise_frequency, stress_level)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, input_data.patient_id, input_data.breakfast, input_data.lunch, input_data.dinner, input_data.irregular_periods,
                 input_data.period_cycle_length, input_data.fast_food_freq, input_data.veg_fruit_freq, input_data.regular_exercise, input_data.exercise_frequency, input_data.stress_level)
                 
        conn.commit()
        conn.close()
        return {"message": "Details form saved successfully"}
    except Exception as e:
        return HTTPException(status_code=500, detail=str(e))

@app.get("/api/nutritionist/patient/{patient_id}/details_form")
def get_patient_details_form(patient_id: str):
    try:
        conn = get_sql_connection()
        cursor = conn.cursor()
        cursor.execute("SELECT breakfast, lunch, dinner, irregular_periods, period_cycle_length, fast_food_freq, veg_fruit_freq, regular_exercise, exercise_frequency, stress_level FROM patient_details_form WHERE patient_id = ?", patient_id)
        row = cursor.fetchone()
        conn.close()
        
        if row:
            return {
                "patient_id": patient_id,
                "breakfast": row[0],
                "lunch": row[1],
                "dinner": row[2],
                "irregular_periods": row[3],
                "period_cycle_length": row[4],
                "fast_food_freq": row[5],
                "veg_fruit_freq": row[6],
                "regular_exercise": row[7],
                "exercise_frequency": row[8],
                "stress_level": row[9]
            }
        return {"error": "Form not found"}
    except Exception as e:
        return HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
