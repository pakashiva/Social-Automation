import json
from pathlib import Path

from langchain_core.messages import SystemMessage, HumanMessage
from langchain_core.tools import tool

from langgraph.graph import MessagesState

from model import llm
from utils.strategy import load_strategy

from agents.planner_agent.planner_prompt import SYSTEM_PROMPT
from agents.planner_agent.planner_schema import PlannerOutput
from agents.planner_agent.init_db import app, db
from agents.planner_agent.models import PlannerHistory


@tool
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

@tool
def read_live_events():

    " Use this tool to read 'live_events.txt' file. And proceed as per the instructions"

    BASE_DIR = Path(__file__).resolve().parent.parent.parent
    filepath = BASE_DIR / "agents" / "planner_agent" / "live_events.txt"

    with open(filepath , "r" , encoding='utf-8') as f:
        return f.read()

@tool
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


tools = [read_live_events , read_content_strategy , get_previous_history]
tool_llm = llm.bind_tools(tools=tools)

def chatbot(state : MessagesState):
    messages = [
        SystemMessage(content=SYSTEM_PROMPT)
        ] + state["messages"]

    res = tool_llm.invoke(messages)
    return {"messages" : [res]}

# tools


def load_planner():

    return llm.with_structured_output(PlannerOutput)


def planner_output_node(state: MessagesState):
    planner = load_planner()

    output = planner.invoke(state["messages"])

    return {
        "planner_output": output
    }
