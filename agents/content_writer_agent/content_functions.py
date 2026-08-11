from agents.content_writer_agent.content_prompt import FACEBOOK_PROMPT , INSTAGRAM_PROMPT , LINKEDIN_PROMPT
from langchain_core.messages import HumanMessage , SystemMessage
from model import llm


def generate_linkedin_content(topic , brand_voice , pillar  , post_format , pillar_guidlines):
    messages = [
    SystemMessage(content=LINKEDIN_PROMPT),
    HumanMessage(content=f"""
    TOPIC
    {topic}

    CONTENT PILLAR
    {pillar}

    BRAND VOICE
    {brand_voice}

    POST FORMAT
    {post_format}

    PILLAR GUIDELINES
    {pillar_guidlines}

    """)
    ]

    generated_content = llm.invoke(messages)
    if generated_content:
        print("CONETENT GENERATED" , generated_content[:30])

    return generated_content

def generate_facebook_content(topic , brand_voice , pillar  , post_format):
    messages = [
    SystemMessage(content=LINKEDIN_PROMPT),
    HumanMessage(content=f"""
    TOPIC
    {topic}

    CONTENT PILLAR
    {pillar}

    BRAND VOICE
    {brand_voice}

    POST FORMAT
    {post_format}

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