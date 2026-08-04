from flask import Flask
from flask_sqlalchemy import SQLAlchemy
from dotenv import load_dotenv

from app_configuration.linkedin_app import LinkedIn_app_config

load_dotenv()

db = SQLAlchemy()

app = Flask(__name__)
app.config.from_object(LinkedIn_app_config)

db.init_app(app)