"""
Functions for the Image Prompt Generator Agent.
"""

from typing import Optional

from langchain_core.messages import SystemMessage, HumanMessage

from agents.image_prompt_generator.prompt import (
    IMAGE_PROMPT_SYSTEM,
    IMAGE_PROMPT_USER,
)


def generate_image_prompt(
    llm,
    content: str,
    platform: str,
) -> str:
    """
    Generate an image-generation prompt from social media content.

    Parameters
    ----------
    llm:
        LangChain-compatible chat model.

    content:
        The final social media post content.

    platform:
        Target social media platform.

    Returns
    -------
    str
        A prompt that can be directly passed to an
        image-generation model.
    """

    if not content or not content.strip():
        raise ValueError(
            "Content is required to generate an image prompt."
        )

    if not platform or not platform.strip():
        raise ValueError(
            "Platform is required to generate an image prompt."
        )

    platform = platform.strip().lower()
    content = content.strip()

    messages = [
        SystemMessage(
            content=IMAGE_PROMPT_SYSTEM
        ),
        HumanMessage(
            content=IMAGE_PROMPT_USER.format(
                platform=platform,
                content=content,
            )
        ),
    ]

    response = llm.invoke(messages)

    image_prompt = response.content

    if isinstance(image_prompt, list):
        image_prompt = "".join(
            block.get("text", "")
            for block in image_prompt
            if isinstance(block, dict)
        )

    image_prompt = image_prompt.strip()

    if not image_prompt:
        raise ValueError(
            "The LLM returned an empty image prompt."
        )

    print(image_prompt)
    
    return image_prompt