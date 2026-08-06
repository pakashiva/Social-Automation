from flask import Flask
from flask_sqlalchemy import SQLAlchemy
from dotenv import load_dotenv

from app_configuration.app_config import Config
load_dotenv()

db = SQLAlchemy()

app = Flask(__name__)
app.config.from_object(Config)

db.init_app(app)