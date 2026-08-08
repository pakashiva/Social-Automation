from initialize_database.models import CompanyInfo
from app import app , db

with app.app_context():
    rows = CompanyInfo.query.all()

    for row in rows:
        print(row.user_id)
        print(row.content_strategy_json)
        print(row.brand_context)
        print(row.scheduled_time)
