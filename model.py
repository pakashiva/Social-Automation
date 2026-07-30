from langchain_ollama import ChatOllama , OllamaEmbeddings

llm = ChatOllama(
    model = 'qwen2.5:7b' , 
    temperature= 0.3
)

embeddings = OllamaEmbeddings(
    model='nomic-embed-text:latest'
)