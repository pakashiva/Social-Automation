from pathlib import Path
from flask import Flask
from flask_sqlalchemy import SQLAlchemy

BASE_DIR = Path(__file__).resolve().parent.parent.parent

# Create the databases directory if it doesn't exist
DATABASE_DIR = BASE_DIR / "databases"
DATABASE_DIR.mkdir(parents=True, exist_ok=True)

DATABASE_PATH = DATABASE_DIR / "planner.db"

app = Flask(__name__)
app.config["SQLALCHEMY_DATABASE_URI"] = f"sqlite:///{DATABASE_PATH}"
app.config["SQLALCHEMY_TRACK_MODIFICATIONS"] = False

db = SQLAlchemy(app)

def initialize_database():
    from agents.planner_agent import models
    with app.app_context():
        db.create_all()
        print(f"Database created at {DATABASE_PATH}")

