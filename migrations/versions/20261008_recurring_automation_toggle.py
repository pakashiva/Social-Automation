"""Add per-company recurring automation toggle.

Revision ID: 20261008_auto_toggle
Revises: 20261007_hitl_approval
Create Date: 2026-10-08
"""
from alembic import op
import sqlalchemy as sa


revision = "20261008_auto_toggle"
down_revision = "20261007_hitl_approval"
branch_labels = None
depends_on = None


def upgrade():
    with op.batch_alter_table("company_information") as batch_op:
        batch_op.add_column(
            sa.Column(
                "recurring_automation_enabled",
                sa.Boolean(),
                server_default=sa.text("true"),
                nullable=False,
            )
        )


def downgrade():
    with op.batch_alter_table("company_information") as batch_op:
        batch_op.drop_column("recurring_automation_enabled")
