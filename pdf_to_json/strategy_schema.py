from pydantic import BaseModel, Field


# ----------------------------------------------------------
# Pillar
# ----------------------------------------------------------

class Pillar(BaseModel):
    name: str
    allocation: int
    objective: str
    example_topics: list[str] = Field(default_factory=list)


# ----------------------------------------------------------
# Brand Voice
# ----------------------------------------------------------

class BrandVoice(BaseModel):
    traits: list[str] = Field(default_factory=list)


# ----------------------------------------------------------
# Post Formats
# ----------------------------------------------------------

class PostFormats(BaseModel):
    formats: list[str] = Field(default_factory=list)


# ----------------------------------------------------------
# Final Strategy
# ----------------------------------------------------------

class Strategy(BaseModel):
    pillars: list[Pillar] = Field(default_factory=list)
    brand_voice: BrandVoice = Field(default_factory=BrandVoice)
    post_formats: PostFormats = Field(default_factory=PostFormats)