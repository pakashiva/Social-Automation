from pdf_to_json.strategy_loader import convert_pdf_to_strategy
from pathlib import Path
import time

file_path = Path("data") / "Pillars_Voice_Format.pdf"


start = time.perf_counter()

strategy = convert_pdf_to_strategy(pdf_path=file_path)

end = time.perf_counter()

strategy_json = strategy.model_dump(mode="json")

print(strategy_json)
print(f"Took {end - start:.2f} seconds")