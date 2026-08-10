from app_configuration.app_config import Config
from initialize_database.models import (User , Account , CompanyInfo , PlannerHistory , PublishedPost)
from app import app, db

def initialize_database():
    with app.app_context():
        db.create_all()
        print(f"Database initialized successfully")