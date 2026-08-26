import json
from pathlib import Path

from langchain_community.document_loaders import PyPDFLoader
from langchain_core.messages import HumanMessage, SystemMessage

from model import llm
from pdf_to_json.strategy_schema import Strategy
from pdf_to_json.pillar_prompt import STRATEGY_PROMPT



def load_pdf(pdf_path: str | Path) -> str:
    loader = PyPDFLoader(str(pdf_path))
    pages = loader.load()
    return "\n".join(page.page_content for page in pages)


strategy_llm = llm.with_structured_output(Strategy)


# extracts the pillar names , allocation , brandvoice , post format from the uploaded pdf.

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

