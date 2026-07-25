from pydantic import BaseModel ,Field

class ContentPillar(BaseModel):
    name : str = Field(description="Content pillar name")
    weight : int=  Field(description="Percentage weight")


class Tone(BaseModel):
    overall: str
    writing_style: str
    personality: str
    technical_depth: str
    language: str
    cta: str
    avoid: list[str]

class Strategy(BaseModel):
    company_name : str
    pillars: list[ContentPillar]
    tone: Tone