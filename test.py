from agents.planner_agent.planner_functions import invoke_planner
from agents.planner_agent.init_db import app , db
from agents.planner_agent.models import PlannerHistory

output = invoke_planner()

print(output)

with app.app_context():
    rows = PlannerHistory.query.all()

    for row in rows:
        print(row.topic , row.tone , row.pillar)

