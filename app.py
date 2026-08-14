import os

from flask import Flask
from flask_sqlalchemy import SQLAlchemy
from dotenv import load_dotenv
from flask_migrate import Migrate
from flask_jwt_extended import JWTManager

from app_configuration.app_config import Config
load_dotenv()

db = SQLAlchemy()

app = Flask(__name__)
app.config.from_object(Config)

db.init_app(app)
from initialize_database import models

migrate = Migrate(app=app , db=db)

app.config["JWT_SECRET_KEY"] = os.getenv("JWT_SECRET_KEY")
app.config["JWT_TOKEN_LOCATION"] = ["cookies"]
app.config["JWT_ACCESS_COOKIE_NAME"] = "access_token"
app.config["JWT_COOKIE_CSRF_PROTECT"] = False   # Enable in production

jwt = JWTManager(app)

from agents.user_topic_generator.routes import (
    register_generate_route,
    register_schedule_route,
)

register_generate_route(app)
register_schedule_route(app)
