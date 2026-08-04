from datetime import datetime , UTC
from app import db

class LinkedInAccount(db.Model):

    __tablename__ = "linkedin_accounts"

    id = db.Column(db.Integer, primary_key=True)

    company_name = db.Column(db.String(100), nullable=False)

    access_token = db.Column(db.Text, nullable=False)

    author_urn = db.Column(db.String(100), nullable=False)

    expires_at = db.Column(db.DateTime, nullable=False)

    created_at = db.Column(
        db.DateTime,
        default=lambda: datetime.now(UTC),
        nullable=False
    )