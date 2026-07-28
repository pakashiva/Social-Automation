import os
import requests
from dotenv import load_dotenv

load_dotenv()

PAGE_ID = os.getenv("FB_PAGE_ID")
PAGE_ACCESS_TOKEN = os.getenv("FB_PAGE_ACCESS_TOKEN")

url = f"https://graph.facebook.com/v23.0/{PAGE_ID}/feed"

def publish_to_facebook(message):
    try:
        response = requests.post(
        url,
        data={
            "message": message,
            "access_token": PAGE_ACCESS_TOKEN,
        },
        )

        print("Status Code:", response.status_code)
        print("\nPost published successfully!")

        response.raise_for_status()
        return response.json()

    except Exception as e:
        return str(e)
