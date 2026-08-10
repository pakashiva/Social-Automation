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

def publish_to_facebook(message , user_id):
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
            user_id = user_id,
            platform="Facebook",
            post_content=message,
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
