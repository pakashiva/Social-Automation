"""Add Meta token expiry tracking.

Revision ID: f1c4d0a8b3e7
Revises: c50fb3ee68e4
Create Date: 2026-10-02

"""
from alembic import op
import sqlalchemy as sa


revision = "f1c4d0a8b3e7"
down_revision = "c50fb3ee68e4"
branch_labels = None
depends_on = None


def upgrade():
    with op.batch_alter_table("accounts", schema=None) as batch_op:
        batch_op.add_column(
            sa.Column("meta_token_expires_at", sa.DateTime(timezone=True), nullable=True)
        )

    with op.batch_alter_table("content_jobs", schema=None) as batch_op:
        batch_op.add_column(sa.Column("images", sa.JSON(), nullable=True))


def downgrade():
    with op.batch_alter_table("content_jobs", schema=None) as batch_op:
        batch_op.drop_column("images")

    with op.batch_alter_table("accounts", schema=None) as batch_op:
        batch_op.drop_column("meta_token_expires_at")
