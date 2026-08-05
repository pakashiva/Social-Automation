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

GOOGLE_API_KEY = os.getenv('GOOGLE_API_KEY')


llm = ChatGoogleGenerativeAI(
    model = 'gemini-2.5-flash',
    temperature = 0.2
)

embeddings = GoogleGenerativeAIEmbeddings(
    model="models/gemini-embedding-001",
    google_api_key=GOOGLE_API_KEY,   # or set GOOGLE_API_KEY env variable
)
