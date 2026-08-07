from app import app , db
from datetime import datetime, UTC
import uuid


# ============================================================
# Users
# ============================================================

class User(db.Model):
    __tablename__ = "users"

    user_id = db.Column(
        db.String(36),
        primary_key=True,
        default=lambda: str(uuid.uuid4())
    )

    email = db.Column(
        db.String(255),
        unique=True,
        nullable=False,
        index=True
    )

    password = db.Column(
        db.String(255),
        nullable=False
    )


# ============================================================
# Social Accounts
# ============================================================

class Account(db.Model):
    __tablename__ = "accounts"

    id = db.Column(db.Integer, primary_key=True)

    user_id = db.Column(
        db.String(36),
        db.ForeignKey("users.user_id"),
        nullable=False
    )

    # Meta
    page_name = db.Column(db.String(255))
    page_id = db.Column(db.String(255))
    page_access_token = db.Column(db.Text)

    instagram_business_id = db.Column(db.String(255))

    # LinkedIn
    linkedin_access_token = db.Column(db.Text)
    author_urn = db.Column(db.String(255))

    linkedin_token_expires_at = db.Column(db.DateTime)



# ============================================================
# Published Posts
# ============================================================

class PublishedPost(db.Model):
    __tablename__ = "published_posts"

    id = db.Column(db.Integer, primary_key=True)

    user_id = db.Column(
        db.String(36),
        db.ForeignKey("users.user_id"),
        nullable=False,
        index=True
    )

    platform = db.Column(
        db.String(30),
        nullable=False
    )

    posted_at = db.Column(
        db.DateTime,
        default=datetime.now(UTC)
    )

    post_content = db.Column(
        db.Text,
        nullable=False
    )


# ============================================================
# Company Information
# ============================================================

class CompanyInfo(db.Model):
    __tablename__ = "company_information"

    id = db.Column(db.Integer, primary_key=True)

    user_id = db.Column(
        db.String(36),
        db.ForeignKey("users.user_id"),
        nullable=False
    )

    brand_context = db.Column(
        db.Text
    )

    # Store file path
    content_strategy_path = db.Column(
        db.String(500)
    )

    content_strategy_json = db.Column(db.JSON)

    scheduled_time = db.Column(
        db.String(50)
    )


# ============================================================
# Planner History
# ============================================================

class PlannerHistory(db.Model):
    __tablename__ = "planner_history"

    id = db.Column(db.Integer, primary_key=True)

    user_id = db.Column(
        db.String(36),
        db.ForeignKey("users.user_id"),
        nullable=False,
        index=True
    )

    pillar = db.Column(
        db.String(255),
        nullable=False
    )

    topic = db.Column(
        db.String(500),
        nullable=False
    )

    brand_voice = db.Column(
        db.String(255)
    )

    post_format = db.Column(
        db.String(100)
    )

    created_at = db.Column(
        db.DateTime,
        default=datetime.now(UTC)
    )