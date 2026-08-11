import os,  chromadb
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
from langchain_chroma import Chroma

# models and embeddings
from model import llm , embeddings

# Retrieval

from langchain_classic.retrievers import EnsembleRetriever
from langchain_community.retrievers import BM25Retriever
from langchain_classic.retrievers import MultiQueryRetriever
from langchain_classic.retrievers.contextual_compression import (
    ContextualCompressionRetriever,
)
from langchain_classic.retrievers.document_compressors import (
    CrossEncoderReranker,
)
from langchain_community.cross_encoders import HuggingFaceCrossEncoder


# Configuration


# BASE_DIR = Path(__file__).resolve().parent.parent
# CHROMA_PATH = BASE_DIR / "databases" / "chromadb"
CHUNK_SIZE = 500
CHUNK_OVERLAP = 20
TOP_K = 5
FINAL_K = 5



client = chromadb.CloudClient(
  api_key=os.getenv('CHROMA_API_KEY'),
  tenant=os.getenv('TENANT_ID'),
  database=os.getenv('CHROMA_DATABASE')
)

# Load PDF

def load_pdf(PDF_PATH) -> List[Document]:
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

def create_vectorstore(chunks: List[Document], COLLECTION_NAME):

    vectorstore = Chroma.from_documents(
        documents=chunks,
        embedding=embeddings,
        collection_name=COLLECTION_NAME,
        client=client
    )

    print("Created Chroma database")
    return vectorstore

# Load Existing Chroma

def load_vectorstore(COLLECTION_NAME):

    vectorstore = Chroma(
        collection_name=COLLECTION_NAME,
        embedding_function=embeddings,
        client=client
    )
    return vectorstore


# Build or Load Database

def build_vector_store(COLLECTION_NAME , PDF_PATH):

    vectorstore = Chroma(
        collection_name=COLLECTION_NAME,
        embedding_function=embeddings,
        client=client
    )

      # If collection exists, delete all existing embeddings
    if vectorstore._collection.count() > 0:
        print(f"Existing collection found. Replacing it...")
        vectorstore.delete_collection()
    
    print("Creating new Chroma Database")
    docs = load_pdf(PDF_PATH)
    chunks = split_documents(docs)
    create_vectorstore(chunks , COLLECTION_NAME)

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


# ====================================================
# hybrid retreiver only
# ====================================================

def create_semantic_retriever(vectorstore:Chroma):
    hybrid = build_hybrid_retriever(
            vectorstore
        )

    return hybrid

def retrieve_semantic_chunks(pillar, user_id):

    # Open the Chroma collection belonging to this user

    vectorstore = Chroma(
        collection_name=str(user_id),
        embedding_function=embeddings,
        client=client
    )

    # Build hybrid retriever for this user's collection
    retriever = create_semantic_retriever(vectorstore)

    # Retrieve using the pillar as the search query
    docs = retriever.invoke(pillar)

    
    data = ""
    for i, doc in enumerate(docs, start=1):
            data += doc.page_content + '\n\n'

    if data != "":
        print("DATA REETRIEVED:" ,  data[:30])
    
    return data