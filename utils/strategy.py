import json
from pathlib import Path


BASE_DIR = Path(__file__).resolve().parent.parent
file_path = BASE_DIR / "databases" / "strategy" / "strategy.json"

def load_strategy():
    with open(file_path , "r") as f:
        return json.load(f)