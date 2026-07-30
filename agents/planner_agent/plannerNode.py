from model import llm

from langgraph.prebuilt import ToolNode , tools_condition
from langchain_core.messages import SystemMessage
from agents.planner_agent.planner_prompt import SYSTEM_PROMPT
from langgraph.graph import START , END , StateGraph , MessagesState
from typing import TypedDict

from utils.strategy import load_strategy
from agents.planner_agent.planner_schema import PlannerOutput
from agents.planner_agent.planner_functions import (
    tools, get_previous_history , read_live_events , load_planner,
    chatbot , save_to_database , planner_output_node
)



class PlannerState(MessagesState):
    planner_output: PlannerOutput | None


builder = StateGraph(PlannerState)
builder.add_node("chatbot" , chatbot)
builder.add_node("planner_output", planner_output_node)
builder.add_node("tools" , ToolNode(tools))

builder.add_edge(START , "chatbot")
builder.add_conditional_edges("chatbot" , tools_condition)
builder.add_edge("tools" , "chatbot")
builder.add_edge("chatbot", "planner_output")
builder.add_edge("planner_output", END)


graph = builder.compile()


def run_planner():
    state = graph.invoke(
        {
            "messages": [
                SystemMessage(
                    content="Generate the next content plan."
                )
            ]
        }
    )
    for m in state["messages"]:
        print("=" * 80)
        print(type(m))
        print(m)

    output = state["planner_output"]  


    save_to_database(output)
    return output





