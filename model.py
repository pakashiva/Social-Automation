from langchain_anthropic import ChatAnthropic
from langchain_google_genai import GoogleGenerativeAIEmbeddings


import os
from dotenv import load_dotenv

load_dotenv()

ANTHROPIC_API_KEY = os.getenv("ANTHROPIC_API_KEY")
ANTHROPIC_MODEL = os.getenv("ANTHROPIC_MODEL", "claude-opus-4-7")
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")


llm = ChatAnthropic(
    model_name=ANTHROPIC_MODEL,
    api_key=ANTHROPIC_API_KEY,
)

gemini_embeddings = GoogleGenerativeAIEmbeddings(
    model="models/gemini-embedding-001",
    api_key=GEMINI_API_KEY,
)

# Gemini embeddings are used for all company collections, old and new.
embeddings = gemini_embeddings
# from langchain_ollama import ChatOllama

# llm = ChatOllama(
#     model='llama3.2:latest',
#     temperature=0.2
#     )
