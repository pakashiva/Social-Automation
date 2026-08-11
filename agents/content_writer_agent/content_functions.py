from agents.content_writer_agent.content_prompt import FACEBOOK_PROMPT , INSTAGRAM_PROMPT , LINKEDIN_PROMPT
from langchain_core.messages import HumanMessage , SystemMessage
from model import llm


def generate_linkedin_content(topic , brand_voice , pillar  , post_format , pillar_guidlines):

    print("ENTERED generate_linkedin_content")

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
    if generated_content.content:
        print("CONETENT GENERATED" , generated_content.content[:30])
    else:
        print("No content Generated")

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

    print("LLM RESPONSE TYPE:", type(generated_content))

    raw_content = generated_content.content

    # The model is returning a list of content blocks.
    # Extract only the actual text block.
    if isinstance(raw_content, list):

        text_parts = []

        for block in raw_content:

            if isinstance(block, dict) and block.get("type") == "text":
                text_parts.append(block.get("text", ""))

        content = "\n".join(text_parts)

    elif isinstance(raw_content, str):

        content = raw_content

    else:

        raise TypeError(
            f"Unexpected content type: {type(raw_content)}"
        )

    if not content.strip():
        raise ValueError("No content generated")

    print("CONTENT GENERATED")
    print("Content type:", type(content))
    print("Content length:", len(content))
    print("Content preview:")
    print(content[:300])

    return content


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