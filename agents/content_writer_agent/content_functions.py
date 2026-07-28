from model import llm
from agents.content_writer_agent.content_prompt import FACEBOOK_PROMPT , INSTAGRAM_PROMPT , LINKEDIN_PROMPT
from langchain_core.messages import HumanMessage , SystemMessage


def generate_linkedin_content(topic , tone , audience , pillar , company_data):
    messages = [
    SystemMessage(content=LINKEDIN_PROMPT),
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

def generate_facebook_content(topic , tone , audience , pillar , company_data):
    messages = [
    SystemMessage(content=FACEBOOK_PROMPT),
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

def generate_instagram_content(topic , tone , audience , pillar , company_data):
    messages = [
    SystemMessage(content=INSTAGRAM_PROMPT),
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