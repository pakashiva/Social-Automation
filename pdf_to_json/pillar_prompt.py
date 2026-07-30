PILLAR_PROMPT = """
Extract exactly one content pillar.

Return a Pillar object.

Extract ONLY these fields.

- name
- allocation
- objective
- example_topics

Rules

1. The pillar name is the title after "Pillar X ---".

2. Allocation is the percentage as an integer.
Example:
Allocation: 20%
becomes
20

3. Objective is the text under the "Objective" heading only.

4. Example Topics is a list containing every topic under the "Example Topics" heading.

5. Stop reading Example Topics when the next heading begins.
Examples of headings include:
- Recommended Structure
- Example
- Rule
- Reference
- Critical Rule
- Original Examples

Ignore every section after Example Topics.

Do not include:
- Recommended Structure
- Example
- Rule
- Critical Rule
- Reference
- Original Examples
- Notes
- Any other headings

Do not summarize.

Do not invent topics.

Return only the structured output.
"""


# ------------------------------------------------------------

VOICE_PROMPT = """
Extract only the Brand Voice section.

Return a BrandVoice object.

Rules

- Extract every voice trait as one list item.
- Ignore everything outside the Brand Voice section.
- Do not invent traits.
- Return only the structured output.
"""


# ------------------------------------------------------------

FORMAT_PROMPT = """
Extract only the Post Formats section.

Return a PostFormats object.

Rules

- Extract every post format as one list item.
- Ignore everything outside the Post Formats section.
- Do not invent formats.
- Return only the structured output.
"""