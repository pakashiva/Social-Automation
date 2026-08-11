import os
from dotenv import load_dotenv
from pathlib import Path

DATABASE_DIR = Path(__file__).resolve().parent.parent

load_dotenv()


class Config:
    # BASE_DIR = Path(__file__).resolve().parent.parent

    # # Create the databases directory if it doesn't exist
    # DATABASE_DIR = BASE_DIR / "databases"
    # DATABASE_DIR.mkdir(parents=True, exist_ok=True)
    # DATABASE_PATH = DATABASE_DIR / "app.db"
    # SQLALCHEMY_DATABASE_URI =  f"sqlite:///{DATABASE_PATH}"

    SECRET_KEY = os.getenv("SECRET_KEY")
    SQLALCHEMY_DATABASE_URI = os.getenv('DATABASE_URL')
    SQLALCHEMY_TRACK_MODIFICATIONS = False

    SQLALCHEMY_ENGINE_OPTIONS = {
    "pool_pre_ping": True,
    "pool_recycle": 1800,
}
