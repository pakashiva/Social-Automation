from pydantic import BaseModel, Field


# ----------------------------------------------------------
# Pillar
# ----------------------------------------------------------

class Pillar(BaseModel):
    name: str
    allocation: int


# ----------------------------------------------------------
# Final Strategy
# ----------------------------------------------------------

class Strategy(BaseModel):
    pillars: list[Pillar] = Field(default_factory=list)
    brand_voice: list[str] = Field(default_factory=list)
    post_formats: list[str] = Field(default_factory=list)