from pydantic import BaseModel, Field

class CronSchema(BaseModel):
    schedule: str = Field(..., description="The schedule string to be converted to cron format.")


