import json
from pathlib import Path

from langchain_core.messages import SystemMessage, HumanMessage

from data.brand_context import elva_brand_context

from utils.strategy import load_strategy

from agents.planner_agent.planner_prompt import SYSTEM_PROMPT
from agents.planner_agent.planner_schema import PlannerOutput
from app import app, db
from initialize_database.models import PlannerHistory

from model import llm

def get_previous_history():

    " Use this tool, to get previously chose pillar ,topics, post format , brand voice. "
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
                "brand_voice": row.brand_voice,
                "post_format" : row.post_format,
                "created_at": row.created_at.isoformat()
            })
        return json.dumps(data, indent=4)


def save_to_database(output):
    with app.app_context():
        db.session.add(
            PlannerHistory(
                pillar=output.pillar,
                topic=output.topic,
                brand_voice=output.brand_voice,
                post_format = output.post_format,
            )
        )
        db.session.commit()

def read_live_events():

    " Use this tool to read 'live_events.txt' file. And proceed as per the instructions"

    BASE_DIR = Path(__file__).resolve().parent.parent.parent
    filepath = BASE_DIR / "agents" / "planner_agent" / "live_events.txt"

    with open(filepath , "r" , encoding='utf-8') as f:
        return f.read()


def read_content_strategy():
    """ Use this tool to read the content strategy which contains 
   - Content pillars
   - Weight (percentage) of each pillar
   - Objective of every pillar
   - Example topics for every pillar
   - Brand voice
   - Post formats  
   """

    return json.dumps(load_strategy() , indent=4)


def load_planner():

    return llm.with_structured_output(PlannerOutput)


def invoke_planner(feedback : str | None = None):
    planner = load_planner()

    live_events = read_live_events()
    previous_planned = get_previous_history()
    content_strategy = read_content_strategy()

    messages = [
        SystemMessage(content=SYSTEM_PROMPT),
        HumanMessage(content=f"Company Context:\n{elva_brand_context}"),
        HumanMessage(content=f"Content Strategy:\n{content_strategy}"),
        HumanMessage(content=f"Previous Planning History:\n{previous_planned}"),
        HumanMessage(content=f"Live Events:\n{live_events}")
    ]


    if feedback:
        messages.append(
            HumanMessage(content=f"Planner Feedback:\n{feedback}")
        )
    
    output = planner.invoke(messages)

    return output


