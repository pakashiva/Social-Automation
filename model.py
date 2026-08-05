# from langchain_ollama import ChatOllama , OllamaEmbeddings
from langchain_google_genai import ChatGoogleGenerativeAI , GoogleGenerativeAIEmbeddings

# llm = ChatOllama(
#     model = 'qwen2.5:7b' , 
#     temperature= 0.3
# )

# embeddings = OllamaEmbeddings(
#     model='nomic-embed-text:latest'
# )

import os
from dotenv import load_dotenv

load_dotenv()

GEMINI_API_KEY = os.getenv('GEMINI_API_KEY')


llm = ChatGoogleGenerativeAI(
    model = 'gemini-2.5-flash',
    temperature = 0.2,
    api_key=GEMINI_API_KEY
)

embeddings = GoogleGenerativeAIEmbeddings(
    model="models/gemini-embedding-001",
    api_key=GEMINI_API_KEY,   # or set GEMINI_API_KEY env variable
)
