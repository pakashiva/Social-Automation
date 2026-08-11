from app import db , app
import requests
from initialize_database.models import PublishedPost , Account

from app import app, db


with app.app_context():
    last_account = Account.query.order_by(Account.id.desc()).first()

    if last_account:
        PAGE_ID = last_account.page_id
        PAGE_ACCESS_TOKEN = last_account.page_access_token
    else:
        PAGE_ID = None
        PAGE_ACCESS_TOKEN = None

url = f"https://graph.facebook.com/v23.0/{PAGE_ID}/feed"

def publish_to_facebook(message, user_id):
    try:
        print("Starting Facebook publishing...")
        print("Message length:", len(message))

        response = requests.post(
            url,
            data={
                "message": message,
                "access_token": PAGE_ACCESS_TOKEN,
            },
            timeout=60
        )

        print("Facebook Status Code:", response.status_code)
        print("Facebook Response:", response.text[:500])

        response.raise_for_status()

        published_post = PublishedPost(
            user_id=user_id,
            platform="Facebook",
            post_content=message,
        )

        with app.app_context():
            db.session.add(published_post)
            db.session.commit()

        print("Post saved to database successfully!")

        return response.json()

    except Exception as e:
        print("FACEBOOK ERROR:", repr(e))
        raise