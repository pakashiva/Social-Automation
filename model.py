from langchain_ollama import ChatOllama , OllamaEmbeddings

llm = ChatOllama(
    model = 'llama3.2:latest' , 
    temperature= 0
)

embeddings = OllamaEmbeddings(
    model='nomic-embed-text:latest'
)