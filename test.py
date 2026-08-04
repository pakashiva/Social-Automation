from init_db.linkedin_accounts.models import LinkedInAccount
from app import db, app


with app.app_context():
    rows = LinkedInAccount.query.all()
    for row in rows:
        print(row.id, row.company_name, row.access_token, row.expires_at, row.created_at)