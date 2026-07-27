import json
from pathlib import Path
from planner.strategy_schema import Strategy
from langchain_ollama import ChatOllama
from langchain_community.document_loaders import PyPDFLoader
from langchain_core.messages import SystemMessage , HumanMessage

MODEL = 'llama3.2:latest'
BASE_DIR = Path(__file__).resolve().parent.parent
file_path = BASE_DIR / "data" / "content_strategy.pdf"

def pdf_loader(file_path):

    loader = PyPDFLoader(file_path)
    try:
        document = loader.load()
    except Exception as e:
        print("Error: ", str(e))

    text = "\n".join(
        page.page_content
        for page in document
    )
    return text

SYSTEM_PROMPT = """ 
You are an information extraction assistant.
Extract the content strategy.
Return ONLY valid JSON.

Include

- company name
- content pillars
- pillar weights
- tone
- writing style
- personality
- language
- CTA
- avoid

Do not invent fields.
"""

# defining llm

llm = ChatOllama(
    model = MODEL, 
    temperature=0
    )

structured_llm = llm.with_structured_output(
        Strategy
    )


def save_json_data(file_path):

    content = pdf_loader(file_path)
    messages = [
        SystemMessage(content=SYSTEM_PROMPT) , 
        HumanMessage(content=content)
    ]

    data = structured_llm.invoke(messages)

    planner_dir = BASE_DIR / "databases" / "strategy"
    planner_dir.mkdir(exist_ok=True)
    output_path = planner_dir / "strategy.json"


    try:
        with open(output_path, "w") as f:
            json.dump(data.model_dump(), f, indent=4)
        print("Data saved successfully in JSON format.")

    except Exception as e:
        print(f"Failed to save data: {e}")