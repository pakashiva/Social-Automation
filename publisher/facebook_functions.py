from app import db , app
import requests
from init_db.published_posts.models import PublishedPost

from init_db.meta_accounts.models import MetaAccount
from app import app, db


with app.app_context():
    last_account = MetaAccount.query.order_by(MetaAccount.id.desc()).first()

    if last_account:
        PAGE_ID = last_account.page_id
        PAGE_ACCESS_TOKEN = last_account.page_access_token
    else:
        PAGE_ID = None
        PAGE_ACCESS_TOKEN = None

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
