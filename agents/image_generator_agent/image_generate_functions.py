# import os , cloudinary , cloudinary.uploader
# from dotenv import load_dotenv
# from langchain_google_genai import ChatGoogleGenerativeAI
# from langchain_core.messages import SystemMessage , HumanMessage
# from agents.image_generator_agent.image_generate_prompt import (SYSTEM_PROMPT_FOR_DECISION ,
#                                                                  SYSTEM_PROMPT_FOR_GENERATION)
# from agents.image_generator_agent.prompt_output import ImageDecision

# load_dotenv()

# cloudinary.config(
#     cloud_name=os.getenv("CLOUDINARY_CLOUD_NAME"),
#     api_key=os.getenv("CLOUDINARY_API_KEY"),
#     api_secret=os.getenv("CLOUDINARY_API_SECRET"),
#     secure=True
# )

# decision_llm = ChatGoogleGenerativeAI(
#     model="gemini-2.5-flash",
#     temperature=0
# )

# image_llm = ChatGoogleGenerativeAI(
#     model="gemini-2.5-flash-image",
#     temperature=0
# )

# def generate_image_decision(topic , pillar , post):

#     messages = [
#         SystemMessage(content=SYSTEM_PROMPT_FOR_DECISION),
#         HumanMessage(content= f"""
# Topic:
# {topic}

# Pillar:
# {pillar}

# Social Media post:
# {post}

# """)
#     ]

#     structured_llm = decision_llm.with_structured_output(ImageDecision)
#     response = structured_llm.invoke(messages)

#     return response


# def get_public_url(image_bytes):
#     try:
#         result = cloudinary.uploader.upload(
#         image_bytes,
#         resource_type="image"
#         )
#         return result['secure_url']
    
#     except Exception as e:
#         return str(e)

# def get_image(topic , pillar , post):

#     response = generate_image_decision(topic=topic , pillar=pillar , post=post)
#     prompt = response.image_prompt
#     reason = response.reason
#     generate_image = response.generate_image

#     if generate_image and not prompt:
#         raise ValueError("Model decided to generate an image but returned no image_prompt.")

#     if generate_image:

#         messages = [
#             SystemMessage(content=SYSTEM_PROMPT_FOR_GENERATION),
#             HumanMessage(content=prompt)
#         ]

#         response = image_llm.invoke(messages)
#         image_bytes = response.content
#         url = get_public_url(image_bytes=image_bytes)
#         return {
#         "generate_image": True,
#         "url": url,
#         "reason": reason,
#     }

#     else:
#         return {
#         "generate_image": False,
#         "url": None,
#         "reason": reason,
#     } 




