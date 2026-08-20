import json
from pathlib import Path
from langchain_core.messages import SystemMessage, HumanMessage
from agents.planner_agent.planner_prompt import SYSTEM_PROMPT
from agents.planner_agent.planner_schema import PlannerOutput
from app import app, db
from initialize_database.models import PlannerHistory, CompanyInfo

from model import llm

def get_previous_history(user_id):
    """
    Use this tool to get previously chosen pillar, topics,
    post format, and brand voice for the user.
    """
    with app.app_context():
        rows = (
            PlannerHistory.query
            .filter_by(user_id=user_id)
            .order_by(PlannerHistory.id.desc())
            .limit(20)
            .all()
        )


        data = []
        for row in reversed(rows):
            data.append({
                "pillar": row.pillar,
                "topic": row.topic,
                "brand_voice": row.brand_voice,
                "post_format" : row.post_format,
                "created_at": row.created_at.isoformat()
            })
        return json.dumps(data, indent=4)


def save_to_database(output , user_id):
    with app.app_context():
        db.session.add(
            PlannerHistory(
                user_id=user_id,
                pillar=output.pillar,
                topic=output.topic,
                brand_voice=output.brand_voice,
                post_format = output.post_format,
            )
        )
        db.session.commit()


def get_stragtegy_and_brand_content(user_id):
    """ Use this tool to read the content strategy which contains 
   - Content pillars
   - Weight (percentage) of each pillar
   - Objective of every pillar
   - Example topics for every pillar
   - Brand voice
   - Post formats  
   """
    company = CompanyInfo.query.filter_by(user_id=user_id).first()

    strategy = company.content_strategy_json
    brand_context = company.brand_context

    return strategy , brand_context


def load_planner():

    return llm.with_structured_output(PlannerOutput)


def invoke_planner(user_id , feedback : str | None = None):
    planner = load_planner()

    previous_planned = get_previous_history(user_id=user_id)
    content_strategy , brand_context = get_stragtegy_and_brand_content(user_id=user_id)

    messages = [
        SystemMessage(content=SYSTEM_PROMPT),
        HumanMessage(content=f"Company Context:\n{brand_context}"),
        HumanMessage(content=f"Content Strategy:\n{content_strategy}"),
        HumanMessage(content=f"Previous Planning History:\n{previous_planned}"),
    ]


    if feedback:
        messages.append(
            HumanMessage(content=f"Planner Feedback:\n{feedback}")
        )
    
    output = planner.invoke(messages)

    return output


