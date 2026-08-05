import os
from dotenv import load_dotenv
from pathlib import Path

from agents.planner_agent.init_db import DATABASE_DIR

load_dotenv()

class Config:
    BASE_DIR = Path(__file__).resolve().parent.parent

    # Create the databases directory if it doesn't exist
    DATABASE_DIR = BASE_DIR / "databases"
    DATABASE_DIR.mkdir(parents=True, exist_ok=True)
    DATABASE_PATH = DATABASE_DIR / "linkedin.db"

    SECRET_KEY = os.getenv("SECRET_KEY")
    SQLALCHEMY_DATABASE_URI = f"sqlite:///{DATABASE_PATH}"
    SQLALCHEMY_TRACK_MODIFICATIONS = False

    SQLALCHEMY_BINDS = {
    "published": f"sqlite:///{DATABASE_DIR / 'published_posts.db'}",
    "meta": f"sqlite:///{DATABASE_DIR / 'meta_accounts.db'}"
}