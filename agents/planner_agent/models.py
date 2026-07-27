from agents.planner_agent.init_db import db

class PlannerHistory(db.Model):
    __tablename__ = "planner_history"

    id = db.Column(db.Integer, primary_key=True)
    pillar = db.Column(db.String(100), nullable=False)
    topic = db.Column(db.String(255), nullable=False)
    tone = db.Column(db.String(100), nullable=False)
    created_at = db.Column(db.DateTime, server_default=db.func.now())