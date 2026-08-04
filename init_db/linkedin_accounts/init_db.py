from app import app, db
from app_configuration.linkedin_app import LinkedIn_app_config

# You only need to import it so SQLAlchemy registers the model.
from init_db.linkedin_accounts.models import LinkedInAccount


def initialize_database():
    with app.app_context():
        print(db.metadata.tables.keys())
        db.create_all()
        print(db.metadata.tables.keys())
        print(
    f"Database initialized successfully at "
    f"{LinkedIn_app_config.DATABASE_PATH}."
) 