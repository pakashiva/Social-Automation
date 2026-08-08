from model import llm
from langchain_core.messages import SystemMessage
from agents.topic_evaluator.eval_prompt import SYSTEM_PROMPT
from agents.topic_evaluator.eval_schema import TopicEvaluation
from agents.planner_agent.planner_functions import invoke_planner
from initialize_database.models import CompanyInfo
from agents.planner_agent.planner_functions import save_to_database
from langchain.messages import HumanMessage


messages = [
    SystemMessage(content=SYSTEM_PROMPT)
]

def evaluate_topic(pillar , topic , brand_context):
    " This function is used to evaluate the topic selected by planner. "

    structured_llm = llm.with_structured_output(TopicEvaluation)

    messages = [
        SystemMessage(content=SYSTEM_PROMPT),
        HumanMessage(
            content=f"""
Brand Context:
{brand_context}

Pillar:
{pillar}

Topic:
{topic}
"""
        ),
    ]

    return structured_llm.invoke(messages)

    response = structured_llm.invoke(messages)

    return response


def generate_topic(user_id):
    feedback = None

    company = CompanyInfo.query.filter_by(user_id=user_id).first()

    for attempt in range(5):
        output = invoke_planner(user_id=user_id , feedback=feedback)

        remarks = evaluate_topic(
            topic=output.topic,
            pillar=output.pillar,
            brand_context=company.brand_context,
        )

        if remarks.approve:
            save_to_database(output=output , user_id=user_id)
            return output

        feedback = f"""
The previous topic was rejected.

Topic:
{output.topic}

Reason:
{remarks.rejection_reasons}

Generate a completely different topic.
Do not repeat or slightly reword the rejected topic.
"""

    raise RuntimeError("Failed to generate an acceptable topic after 5 attempts.")