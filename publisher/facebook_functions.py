import os
from app import db , app
import requests
from dotenv import load_dotenv
from init_db.published_posts.models import PublishedPost
import datetime

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

        pulished_post = PublishedPost(
            platform="Facebook",
            caption=message,
            linkedin_post_id=response.json().get("id"),
        )

        try:
            with app.app_context():
                db.session.add(pulished_post)
                db.session.commit()
                print("Post saved to database successfully!")
        except Exception as e:
            db.session.rollback()
            print("Error saving to database:", str(e))
            raise

        return response.json()

    except Exception as e:
        return str(e)
