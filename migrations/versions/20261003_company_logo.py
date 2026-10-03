"""Add organization logo path for generated image branding.

Revision ID: 20261003_company_logo
Revises: 20261003_posts_pending
Create Date: 2026-10-03
"""
from alembic import op
import sqlalchemy as sa


revision = "20261003_company_logo"
down_revision = "20261003_posts_pending"
branch_labels = None
depends_on = None


def upgrade():
    with op.batch_alter_table("company_information") as batch_op:
        batch_op.add_column(sa.Column("logo_path", sa.String(length=500), nullable=True))


def downgrade():
    with op.batch_alter_table("company_information") as batch_op:
        batch_op.drop_column("logo_path")
