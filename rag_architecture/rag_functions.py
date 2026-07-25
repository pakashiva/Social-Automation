import os
from pathlib import Path
from typing import List

from dotenv import load_dotenv
load_dotenv()

from langchain_core.documents import Document
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_community.document_loaders import PyPDFLoader
from langchain_openai import ChatOpenAI
from langchain_openai import OpenAIEmbeddings
from langchain_chroma import Chroma


# Retrieval

from langchain_classic.retrievers import ContextualCompressionRetriever,BM25Retriever,EnsembleRetriever
from langchain_classic.retrievers import MultiQueryRetriever
from langchain_classic.retrievers.contextual_compression import (
    CrossEncoderReranker,
)
from langchain_community.cross_encoders import HuggingFaceCrossEncoder


# Configuration

PDF_PATH = "data/company_docs.pdf"
CHROMA_PATH = "databases/chromadb"
COLLECTION_NAME = "company_documents"
EMBEDDING_MODEL = "text-embedding-3-small"
LLM_MODEL = "gpt-4.1"
CHUNK_SIZE = 1000
CHUNK_OVERLAP = 200
TOP_K = 20
FINAL_K = 5

# Embeddings


embeddings = OpenAIEmbeddings(
    model=EMBEDDING_MODEL
)


# LLM

llm = ChatOpenAI(
    model=LLM_MODEL,
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

    chroma_exists = (
        Path(CHROMA_PATH).exists()
        and any(Path(CHROMA_PATH).iterdir())
    )

    if chroma_exists:

        return load_vectorstore()

    print("No Chroma database found")
    documents = load_pdf()
    chunks = split_documents(documents)
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


# Multi Query Prompt

MULTI_QUERY_PROMPT = ChatPromptTemplate.from_messages(
    [
        (
            "system",

            """
You are an expert query expansion assistant.
Generate FIVE different search queries.

Each query should retrieve
different relevant information.

Only return the queries.
One query per line.
Do not number them.
            """
),
        (
            "human",
            "{question}"
        ),
    ]
)

# Multi Query Retriever

def build_multi_query_retriever(
    hybrid_retriever,
):
    print("Creating Muti Query Retriever...")

    multi = MultiQueryRetriever.from_llm(
        retriever=hybrid_retriever,
        llm=llm,
        prompt=MULTI_QUERY_PROMPT,
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

# Complete Retrieval Pipeline

def create_retriever(
    vectorstore: Chroma,
):

    hybrid = build_hybrid_retriever(vectorstore)
    multi = build_multi_query_retriever(hybrid)
    return multi

# Cross Encoder Model

def load_cross_encoder():
    """
    Load HuggingFace Cross Encoder.
    """
    print("Loading Cross Encoder...")
    model = HuggingFaceCrossEncoder(
        model_name="BAAI/bge-reranker-base"
    )
    return model

# Cross Encoder Reranker

def build_reranker():
    """
    Re-rank retrieved documents and keep top FINAL_K.
    """

    cross_encoder = load_cross_encoder()
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