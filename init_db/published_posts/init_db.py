from app import app, db
from app_configuration.linkedin_app import Config

# You only need to import it so SQLAlchemy registers the model.
from init_db.published_posts.models import PublishedPost

def initialize_database():
    with app.app_context():
        print(db.metadata.tables.keys())
        db.create_all()
        print(db.metadata.tables.keys())
        print(
    f"Database initialized successfully at "
    f"{Config.DATABASE_PATH}."
) 