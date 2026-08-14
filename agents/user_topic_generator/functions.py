from langchain_core.messages import HumanMessage, SystemMessage

from agents.user_topic_generator.content_prompt import (
    FACEBOOK_PROMPT,
    INSTAGRAM_PROMPT,
    LINKEDIN_PROMPT,
)
from model import llm


SOURCE_LABELS = {
    "inspiration": (
        "Inspiration — create a complete post from the user's idea or direction."
    ),
    "existing_post": (
        "Existing Post — refine the provided post while preserving its original meaning."
    ),
    "generate": (
        "Generate New Content — create original content using brand context "
        "and any optional direction from the user."
    ),
}


def _extract_chunk_text(chunk):
    content = getattr(chunk, "content", None)

    if content is None:
        text = getattr(chunk, "text", None)
        return text or ""

    if isinstance(content, str):
        return content

    if isinstance(content, list):
        parts = []

        for block in content:
            if isinstance(block, dict) and block.get("type") == "text":
                parts.append(block.get("text", ""))
            elif isinstance(block, str):
                parts.append(block)

        return "".join(parts)

    return str(content)


def _build_human_message(user_input, content_source, brand_context):
    source_label = SOURCE_LABELS.get(
        content_source,
        content_source or "generate",
    )

    return HumanMessage(
        content=f"""CONTENT SOURCE
{source_label}

USER INPUT
{user_input.strip() if user_input and user_input.strip() else "(none)"}

BRAND CONTEXT
{brand_context.strip() if brand_context and str(brand_context).strip() else "(not provided)"}
"""
    )


def _stream_post(system_prompt, user_input, content_source, brand_context=None):
    messages = [
        SystemMessage(content=system_prompt),
        _build_human_message(user_input, content_source, brand_context),
    ]

    for chunk in llm.stream(messages):
        text = _extract_chunk_text(chunk)

        if text:
            yield text


def generate_linkedin_post(user_input, content_source, brand_context=None):
    """Stream a LinkedIn post for the selected content source."""

    yield from _stream_post(
        LINKEDIN_PROMPT,
        user_input,
        content_source,
        brand_context,
    )


def generate_instagram_post(user_input, content_source, brand_context=None):
    """Stream an Instagram caption for the selected content source."""

    yield from _stream_post(
        INSTAGRAM_PROMPT,
        user_input,
        content_source,
        brand_context,
    )


def generate_facebook_post(user_input, content_source, brand_context=None):
    """Stream a Facebook post for the selected content source."""

    yield from _stream_post(
        FACEBOOK_PROMPT,
        user_input,
        content_source,
        brand_context,
    )


PLATFORM_GENERATORS = {
    "linkedin": generate_linkedin_post,
    "instagram": generate_instagram_post,
    "facebook": generate_facebook_post,
}


def stream_generated_content(
    platform,
    user_input,
    content_source,
    brand_context=None,
):
    generator = PLATFORM_GENERATORS.get((platform or "").lower())

    if generator is None:
        raise ValueError(f"Unsupported platform: {platform}")

    yield from generator(
        user_input=user_input,
        content_source=content_source,
        brand_context=brand_context,
    )
