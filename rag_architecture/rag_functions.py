import os
from pathlib import Path
from typing import List

from dotenv import load_dotenv
load_dotenv()

from langchain_core.documents import Document
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.runnables import RunnablePassthrough
from langchain_core.output_parsers import StrOutputParser
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_community.document_loaders import PyPDFLoader
from langchain_openai import ChatOpenAI
from langchain_openai import OpenAIEmbeddings
from langchain_chroma import Chroma

# ollma
from langchain_ollama import ChatOllama , OllamaEmbeddings

# Retrieval

from langchain_classic.retrievers import BM25Retriever,EnsembleRetriever
from langchain_classic.retrievers import MultiQueryRetriever
from langchain_classic.retrievers.contextual_compression import (
    ContextualCompressionRetriever,
)
from langchain_classic.retrievers.document_compressors import (
    CrossEncoderReranker,
)
from langchain_community.cross_encoders import HuggingFaceCrossEncoder


# Configuration

PDF_PATH = "data/company_profile.pdf"
CHROMA_PATH = "databases/chromadb"
COLLECTION_NAME = "company_documents"  # like table names in SQL
EMBEDDING_MODEL = "text-embedding-3-small"
LLM_MODEL = "gpt-4.1"
CHUNK_SIZE = 1000
CHUNK_OVERLAP = 200
TOP_K = 20
FINAL_K = 5

# Embeddings

# embeddings = OpenAIEmbeddings(
#     model=EMBEDDING_MODEL
# )

embeddings = OllamaEmbeddings(
    model = 'nomic-embed-text:latest'
)

# LLM

# llm = ChatOpenAI(
#     model=LLM_MODEL,
#     temperature=0
# )

llm = ChatOllama(
    model='llama3.2:latest' , 
    temperature=0
)

# Load PDF

def load_pdf() -> List[Document]:
    """
    Load the company PDF.
    """

    loader = PyPDFLoader(PDF_PATH)
    documents = loader.load()
    print(f"\nLoaded {len(documents)} pages")
    return documents

# Split Documents

def split_documents(
    documents: List[Document],
) -> List[Document]:

    splitter = RecursiveCharacterTextSplitter(
        chunk_size=CHUNK_SIZE,
        chunk_overlap=CHUNK_OVERLAP,
        separators=[
            "\n\n",
            "\n",
            ". ",
            " ",
            ""
        ],
    )

    chunks = splitter.split_documents(documents)
    print(f"Generated {len(chunks)} chunks")
    return chunks

# Create Chroma Database

def create_vectorstore(chunks: List[Document]):

    vectorstore = Chroma.from_documents(
        documents=chunks,
        embedding=embeddings,
        collection_name=COLLECTION_NAME,
        persist_directory=CHROMA_PATH,
    )

    print("Created Chroma database")
    return vectorstore

# Load Existing Chroma

def load_vectorstore():

    vectorstore = Chroma(
        collection_name=COLLECTION_NAME,
        embedding_function=embeddings,
        persist_directory=CHROMA_PATH,
    )

    print("Loaded existing Chroma")

    return vectorstore


# Build or Load Database

def get_vectorstore():

    vectorstore = Chroma(
        collection_name=COLLECTION_NAME,
        embedding_function=embeddings,
        persist_directory=CHROMA_PATH,
    )

    if vectorstore._collection.count() > 0:
        print("Loaded existing Chroma Database")
        return vectorstore
    
    print("Creating new Chroma Database")
    docs = load_pdf()
    chunks = split_documents(docs)
    return create_vectorstore(chunks)

# Build BM25 Retriever

def build_bm25_retriever(
    vectorstore: Chroma,
):
    """
    Build a BM25 retriever using all documents
    stored in the Chroma database.
    """

    print("Building BM25 Retriever...")
    documents = vectorstore.get()
    docs = []
    for text, metadata in zip(
        documents["documents"],
        documents["metadatas"]
    ):
        docs.append(
            Document(
                page_content=text,
                metadata=metadata,
            )
        )

    bm25 = BM25Retriever.from_documents(docs)
    bm25.k = TOP_K
    print(f"BM25 indexed {len(docs)} chunks")
    return bm25

# Dense Retriever

def build_dense_retriever(
    vectorstore: Chroma,
):

    retriever = vectorstore.as_retriever(
        search_type="similarity",
        search_kwargs={
            "k": TOP_K
        },
    )
    return retriever

# Hybrid Search

def build_hybrid_retriever(
    vectorstore: Chroma,
):

    print("Creating Hybrid Retriever...")
    dense = build_dense_retriever(vectorstore)
    bm25 = build_bm25_retriever(vectorstore)
    hybrid = EnsembleRetriever(
        retrievers=[dense, bm25],
        weights=[0.5, 0.5], #You can later tune the weights
    )

    return hybrid

# Multi Query Retriever

def build_multi_query_retriever(
    hybrid_retriever,
):
    print("Creating Muti Query Retriever...")

    multi = MultiQueryRetriever.from_llm(
        retriever=hybrid_retriever,
        llm=llm,
    )
    return multi

# Retrieve Documents

def retrieve_documents(
    retriever,
    question: str,
):

    docs = retriever.invoke(question)
    docs = docs[:TOP_K]
    print(f"\nRetrieved {len(docs)} documents")
    return docs

# Cross Encoder Reranker

def build_reranker():
    """
    Re-rank retrieved documents and keep top FINAL_K.
    """

    cross_encoder = HuggingFaceCrossEncoder(
        model_name="BAAI/bge-reranker-base"
    )

    reranker = CrossEncoderReranker(
        model=cross_encoder,
        top_n=FINAL_K,
    )
    return reranker

# Contextual Compression Retriever

def build_contextual_compression_retriever(
    base_retriever,
):

    print("Creating Contextual Compression Retriever...")
    reranker = build_reranker()
    compression_retriever = ContextualCompressionRetriever(
        base_compressor=reranker,
        base_retriever=base_retriever,
    )
    return compression_retriever

# Complete Retriever Pipeline

def create_retriever(
    vectorstore: Chroma,
):

    hybrid = build_hybrid_retriever(
        vectorstore
    )
    multi_query = build_multi_query_retriever(
        hybrid
    )
    compression = build_contextual_compression_retriever(
        multi_query
    )
    return compression


# Retrieve Final Context

def get_context(
    retriever,
    question,
):
    docs = retriever.invoke(question)
    print(
        f"\nRetrieved {len(docs)} final documents."
    )
    return docs

# Format Context

def format_docs(
    docs,
):
    return "\n\n".join(
        doc.page_content
        for doc in docs
    )

# Prompt Template

RAG_PROMPT = ChatPromptTemplate.from_template(
"""
You are an AI assistant for the company.
Answer ONLY using the provided context.
If the answer is not present in the context, simply reply:
"I don't have enough information in the company documents."
Do not make up information.

------------------------
Context:
{context}
------------------------

Question:
{question}

Answer:
"""
)

# LCEL RAG Chain

def create_rag_chain(retriever):
    chain = (
        {
            "context": retriever | format_docs,
            "question": RunnablePassthrough(),
        }
        | RAG_PROMPT
        | llm
        | StrOutputParser()
    )
    return chain

# Build Entire RAG System

def initialize_rag():

    print("\nInitializing RAG System...\n")
    vectorstore = get_vectorstore()
    retriever = create_retriever(vectorstore)
    rag_chain = create_rag_chain(retriever)
    print("\nRAG Ready!\n")
    return rag_chain


# Ask Question

def ask_question(
    rag_chain,
    question,
):
    answer = rag_chain.invoke(question)
    return answer

# Debug Retrieval

def inspect_retrieval(
        question,
):
    vectorstore = get_vectorstore()
    retriever = create_retriever(
        vectorstore
    )

    docs = retriever.invoke(
        question
    )
    print("\nRetrieved Documents:\n")
    for i, doc in enumerate(docs, start=1):
        print("=" * 80)
        print(f"Chunk {i}")
        print("=" * 80)
        print(doc.page_content)
        print("\n")