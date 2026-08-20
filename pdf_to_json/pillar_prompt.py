STRATEGY_PROMPT = """
Extract the strategy information from the provided document.

Return ONLY the following JSON structure:

{
    "pillars": [
        {
            "name": "string",
            "allocation": 0
        }
    ],
    "brand_voice": [],
    "post_formats": []
}

Rules:

PILLARS:
- Extract every content pillar.
- "name" is the pillar name from the pillar heading.
- "allocation" is the allocation percentage as an integer.
- Example: 20% -> 20.
- Do not extract objective.
- Do not extract example topics.

BRAND VOICE:
- Extract every brand voice trait as a string in brand_voice.
- Do not invent traits.

POST FORMATS:
- Extract every post format as a string in post_formats.
- Do not invent formats.

Do not summarize.
Do not add any other fields.
Return only the structured output.
"""