"""Store scheduled post editing and regeneration data.

Revision ID: 20261003_posts_pending
Revises: f1c4d0a8b3e7
Create Date: 2026-10-03
"""
from alembic import op
import sqlalchemy as sa


revision = "20261003_posts_pending"
down_revision = "f1c4d0a8b3e7"
branch_labels = None
depends_on = None


def upgrade():
    with op.batch_alter_table("content_jobs") as batch_op:
        batch_op.add_column(sa.Column("generation_source", sa.String(length=40), nullable=True))
        batch_op.add_column(sa.Column("generation_input", sa.Text(), nullable=True))
        batch_op.add_column(sa.Column("regeneration_count", sa.Integer(), nullable=False, server_default="0"))
        batch_op.add_column(sa.Column("regeneration_date", sa.Date(), nullable=True))

    with op.batch_alter_table("recurring_content") as batch_op:
        batch_op.add_column(sa.Column("images", sa.JSON(), nullable=True))
        batch_op.add_column(sa.Column("generation_context", sa.JSON(), nullable=True))
        batch_op.add_column(sa.Column("regeneration_count", sa.Integer(), nullable=False, server_default="0"))
        batch_op.add_column(sa.Column("regeneration_date", sa.Date(), nullable=True))


def downgrade():
    with op.batch_alter_table("recurring_content") as batch_op:
        batch_op.drop_column("regeneration_date")
        batch_op.drop_column("regeneration_count")
        batch_op.drop_column("generation_context")
        batch_op.drop_column("images")

    with op.batch_alter_table("content_jobs") as batch_op:
        batch_op.drop_column("regeneration_date")
        batch_op.drop_column("regeneration_count")
        batch_op.drop_column("generation_input")
        batch_op.drop_column("generation_source")
