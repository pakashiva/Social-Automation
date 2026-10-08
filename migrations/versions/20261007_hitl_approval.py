"""Add scheduled post approval and notification preferences.

Revision ID: 20261007_hitl_approval
Revises: 20261003_company_logo
Create Date: 2026-10-07
"""
from alembic import op
import sqlalchemy as sa


revision = "20261007_hitl_approval"
down_revision = "20261003_company_logo"
branch_labels = None
depends_on = None


def upgrade():
    with op.batch_alter_table("company_information") as batch_op:
        batch_op.add_column(sa.Column("notify_hours_before", sa.Integer(), nullable=True))
        batch_op.add_column(sa.Column("publish_if_unapproved", sa.Boolean(), server_default=sa.text("true"), nullable=False))

    for table in ("content_jobs", "recurring_content"):
        with op.batch_alter_table(table) as batch_op:
            batch_op.add_column(sa.Column("hitl_required", sa.Boolean(), server_default=sa.text("false"), nullable=False))
            batch_op.add_column(sa.Column("notify_hours_before", sa.Integer(), nullable=True))
            batch_op.add_column(sa.Column("publish_if_unapproved", sa.Boolean(), server_default=sa.text("true"), nullable=False))
            batch_op.add_column(sa.Column("approval_status", sa.String(length=20), server_default="not_required", nullable=False))
            batch_op.add_column(sa.Column("approved_at", sa.DateTime(timezone=True), nullable=True))


def downgrade():
    for table in ("recurring_content", "content_jobs"):
        with op.batch_alter_table(table) as batch_op:
            batch_op.drop_column("approved_at")
            batch_op.drop_column("approval_status")
            batch_op.drop_column("publish_if_unapproved")
            batch_op.drop_column("notify_hours_before")
            batch_op.drop_column("hitl_required")

    with op.batch_alter_table("company_information") as batch_op:
        batch_op.drop_column("publish_if_unapproved")
        batch_op.drop_column("notify_hours_before")
