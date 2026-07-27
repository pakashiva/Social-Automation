from model import llm
from agents.content_writer_agent.content_prompt import SYSTEM_PROMPT
from langchain_core.messages import HumanMessage , SystemMessage


def generate_content(topic , tone , audience , pillar , company_data):
    messages = [
    SystemMessage(content=SYSTEM_PROMPT),
    HumanMessage(content=f"""
    TOPIC
    {topic}

    CONTENT PILLAR
    {pillar}

    TONE
    {tone}

    TARGET AUDIENCE
    {audience}

    COMPANY KNOWLEDGE
    {company_data}
        
    """)
    ]

    generated_content = llm.invoke(messages)

    return generated_content




    