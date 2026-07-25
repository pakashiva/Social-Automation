from rag_functions import (initialize_rag , ask_question , inspect_retrieval)

rag = initialize_rag()

while True:

    question = input("\nYou: ")
    if question.lower() == "exit":
        break
    answer = ask_question(
        rag,
        question,
    )
    print("Retrieved Chunks: ")
    inspect_retrieval(question)

    print("\nAssistant:\n")
    print(answer)