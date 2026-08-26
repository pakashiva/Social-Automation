from langchain_google_genai import ChatGoogleGenerativeAI , GoogleGenerativeAIEmbeddings

import os
from dotenv import load_dotenv

load_dotenv()

GEMINI_API_KEY = os.getenv('GEMINI_API_KEY')


llm = ChatGoogleGenerativeAI(
    model = 'gemini-3.6-flash',
    temperature = 0.2,
    api_key=GEMINI_API_KEY
)

embeddings = GoogleGenerativeAIEmbeddings(
    model="models/gemini-embedding-001",
    api_key=GEMINI_API_KEY,   # or set GEMINI_API_KEY env variable
)
