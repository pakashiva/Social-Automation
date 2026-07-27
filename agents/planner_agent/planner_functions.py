import json
from pathlib import Path

from langchain_core.messages import SystemMessage, HumanMessage
from model import llm

from agents.planner_agent.planner_prompt import SYSTEM_PROMPT
from agents.planner_agent.planner_schema import PlannerOutput
from agents.planner_agent.init_db import app, db
from agents.planner_agent.models import PlannerHistory
from utils.strategy import load_strategy


def get_planner_history():
    with app.app_context():
        rows = (
            PlannerHistory.query
            .order_by(PlannerHistory.id.desc())
            .limit(20)
            .all()
        )

        data = []
        for row in reversed(rows):
            data.append({
                "pillar": row.pillar,
                "topic": row.topic,
                "tone": row.tone,
                "created_at": row.created_at.isoformat()
            })
        return data

def load_planner():

    planner = llm.with_structured_output(
        PlannerOutput
    )

    return planner

def save_to_database(output):
    with app.app_context():
        db.session.add(
            PlannerHistory(
                pillar=output.pillar,
                topic=output.topic,
                tone=output.tone,
            )
        )
        db.session.commit()

def invoke_planner():

    strategy = json.dumps(load_strategy(), indent=4)
    previous_data = json.dumps(get_planner_history(), indent=4)
    
    planner = load_planner()
    messages = [

        SystemMessage(
            content=SYSTEM_PROMPT
        ),
        HumanMessage(
            content=f"""
    PREVIOUSLY PLANNED CONTENT
    {previous_data}
    ----------------------------------------

    CONTENT STRATEGY
    {strategy}
    """
    )
    ]
    planner_output = planner.invoke(messages)

    save_to_database(planner_output)

    return planner_output
