# from model import llm
from cron_converter.crons_schema import CronSchema
from cron_converter.cron_prompt import SYSTEM_PROMPT
from langchain.messages import SystemMessage, HumanMessage, AIMessage
from croniter import croniter
from datetime import datetime
from dotenv import load_dotenv
from model import llm

structured_llm = llm.with_structured_output(CronSchema)


messages = [
    SystemMessage(content=SYSTEM_PROMPT),]

# converts human-readabel instructions into crons exprssion with help of an llm.

def convert_to_cron(input_string:str) -> str:
    """
    Convert a human-readable schedule into a cron expression using the LLM.

    Args:
        schedule (str): The human-readable schedule string.

    Returns:
        str: The corresponding cron expression or 'INVALID' if it cannot be represented.
    """
    messages.append(HumanMessage(content=input_string))

    response = structured_llm.invoke(messages)

    return response.schedule

# Cron expression validation.
def validate_cron(cron_expression: str) -> bool:
    """
    Validate a standard 5-field cron expression.

    Returns:
        True  -> Valid cron
        False -> Invalid cron
    """
    try:
        croniter(cron_expression, datetime.now())
        return True
    except (ValueError, KeyError):
        return False