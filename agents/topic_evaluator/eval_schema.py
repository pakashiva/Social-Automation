from pydantic import BaseModel , Field

class TopicEvaluation(BaseModel):
    approve : bool
    rate : int
    rejection_reasons : list[str] = Field(
        description="Clear reasons why the topic was rejected."
    )

