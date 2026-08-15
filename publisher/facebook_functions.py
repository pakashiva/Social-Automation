from app import db , app
import requests
from initialize_database.models import PublishedPost , Account
from app import app, db


def publish_to_facebook(message , user_id):

    with app.app_context():
        account = Account.query.filter(user_id=user_id).first()

    PAGE_ACCESS_TOKEN = account.page_access_token
    PAGE_ID = account.page_id

    if not account:
        raise ValueError("No Facebook account found")

    if not PAGE_ID:
        raise ValueError("Facebook PAGE_ID is missing")

    if not PAGE_ACCESS_TOKEN:
        raise ValueError("Facebook PAGE_ACCESS_TOKEN is missing")

    url = f"https://graph.facebook.com/v23.0/{PAGE_ID}/feed"

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