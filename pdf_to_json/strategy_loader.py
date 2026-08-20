import json
from pathlib import Path

from langchain_community.document_loaders import PyPDFLoader
from langchain_core.messages import HumanMessage, SystemMessage

from model import llm
from pdf_to_json.strategy_schema import Strategy
from pdf_to_json.pillar_prompt import STRATEGY_PROMPT


BASE_DIR = Path(__file__).resolve().parent.parent


def load_pdf(pdf_path: str | Path) -> str:
    loader = PyPDFLoader(str(pdf_path))
    pages = loader.load()
    return "\n".join(page.page_content for page in pages)


strategy_llm = llm.with_structured_output(Strategy)


def convert_pdf_to_strategy(pdf_path: str | Path) -> Strategy:

    pdf_text = load_pdf(pdf_path)

    print("Processing strategy...")

    strategy = strategy_llm.invoke(
        [
            SystemMessage(content=STRATEGY_PROMPT),
            HumanMessage(content=pdf_text),
        ]
    )

    return strategy


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