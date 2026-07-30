import json
import re
from pathlib import Path

from langchain_community.document_loaders import PyPDFLoader
from langchain_core.messages import HumanMessage, SystemMessage

from model import llm
from pdf_to_json.pillar_prompt import (
    PILLAR_PROMPT,
    VOICE_PROMPT,
    FORMAT_PROMPT,
)
from pdf_to_json.strategy_schema import (
    Strategy,
    Pillar,
    BrandVoice,
    PostFormats,
)

BASE_DIR = Path(__file__).resolve().parent.parent


# ==========================================================
# PDF
# ==========================================================

def load_pdf(pdf_path: str | Path) -> str:
    loader = PyPDFLoader(str(pdf_path))
    pages = loader.load()
    return "\n".join(page.page_content for page in pages)


# ==========================================================
# Split Sections
# ==========================================================

SECTION_PATTERN = re.compile(
    r"(Pillar\s+\d+\s+---.*?|Brand Voice|Post Formats)",
    flags=re.IGNORECASE,
)


def split_sections(text: str):
    matches = list(SECTION_PATTERN.finditer(text))
    sections = []

    for i, match in enumerate(matches):
        start = match.start()
        end = matches[i + 1].start() if i + 1 < len(matches) else len(text)

        sections.append(
            {
                "title": match.group().strip(),
                "content": text[start:end].strip(),
            }
        )

    return sections


# ==========================================================
# Structured LLMs
# ==========================================================

pillar_llm = llm.with_structured_output(Pillar)
voice_llm = llm.with_structured_output(BrandVoice)
format_llm = llm.with_structured_output(PostFormats)


# ==========================================================
# Extractors
# ==========================================================

def extract_pillar(text: str) -> Pillar:
    return pillar_llm.invoke(
        [
            SystemMessage(content=PILLAR_PROMPT),
            HumanMessage(content=text),
        ]
    )


def extract_brand_voice(text: str) -> BrandVoice:
    return voice_llm.invoke(
        [
            SystemMessage(content=VOICE_PROMPT),
            HumanMessage(content=text),
        ]
    )


def extract_post_formats(text: str) -> PostFormats:
    return format_llm.invoke(
        [
            SystemMessage(content=FORMAT_PROMPT),
            HumanMessage(content=text),
        ]
    )


# ==========================================================
# Convert PDF
# ==========================================================

def convert_pdf_to_strategy(pdf_path: str | Path) -> Strategy:

    pdf_text = load_pdf(pdf_path)

    sections = split_sections(pdf_text)

    strategy = Strategy()

    for section in sections:

        title = section["title"].lower()
        content = section["content"]

        print(f"Processing: {section['title']}")

        if title.startswith("pillar"):
            strategy.pillars.append(
                extract_pillar(content)
            )

        elif "brand voice" in title:
            strategy.brand_voice = extract_brand_voice(content)

        elif "post formats" in title:
            strategy.post_formats = extract_post_formats(content)

    return strategy


# ==========================================================
# Save JSON
# ==========================================================

def save_strategy(pdf_path: str | Path):

    strategy = convert_pdf_to_strategy(pdf_path)

    output_dir = BASE_DIR / "databases" / "strategy"
    output_dir.mkdir(parents=True, exist_ok=True)

    output_file = output_dir / "strategy.json"

    with open(output_file, "w", encoding="utf-8") as f:
        json.dump(
            strategy.model_dump(),
            f,
            indent=4,
            ensure_ascii=False,
        )

    print(f"Saved → {output_file}")
