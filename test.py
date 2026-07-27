from time import perf_counter

start = perf_counter()

from agents.planner_agent.planner_functions import invoke_planner
from rag_system.rag_functions import retrive_chunks
from agents.content_writer_agent.content_functions import generate_content


planner_output = invoke_planner()
topic = planner_output.topic
tone = planner_output.tone
pillar = planner_output.pillar
audience = planner_output.audience

company_data = retrive_chunks(topic=topic)
content = generate_content(
    topic=topic , tone=tone , audience=audience , pillar=pillar , company_data=company_data
)

print(content.content)

end = perf_counter()
print(f"\nExecution time: {end - start:.2f} seconds")