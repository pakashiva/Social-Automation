import os
from dotenv import load_dotenv
from pathlib import Path

load_dotenv()

class LinkedIn_app_config:
    BASE_DIR = Path(__file__).resolve().parent.parent

    # Create the databases directory if it doesn't exist
    DATABASE_DIR = BASE_DIR / "databases"
    DATABASE_DIR.mkdir(parents=True, exist_ok=True)
    DATABASE_PATH = DATABASE_DIR / "linkedin.db"

    SECRET_KEY = os.getenv("SECRET_KEY")
    SQLALCHEMY_DATABASE_URI = f"sqlite:///{DATABASE_PATH}"
    SQLALCHEMY_TRACK_MODIFICATIONS = False