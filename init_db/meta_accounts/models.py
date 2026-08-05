from datetime import datetime , UTC
from app import db


class MetaAccount(db.Model):

    __bind_key__ = "meta"

    __tablename__ = "meta_accounts"

    id = db.Column(db.Integer, primary_key=True)

    page_name = db.Column(db.String(100), nullable=False)

    page_id = db.Column(db.String(100), nullable=False)

    page_access_token = db.Column(db.Text, nullable=False)

    instagram_business_id = db.Column(db.String(100), nullable=True)

    created_at = db.Column(
        db.DateTime,
        default=lambda: datetime.now(UTC),
        nullable=False
    )