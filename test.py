for i in range(5):
    from dotenv import load_dotenv
    import os

    load_dotenv()

    print(os.getenv("CHROMA_API_KEY"))