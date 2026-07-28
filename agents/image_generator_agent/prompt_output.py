from pydantic import BaseModel

class ImageDecision(BaseModel):
    generate_image: bool
    reason: str
    image_prompt: str | None