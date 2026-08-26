from app import db , app
import requests
from initialize_database.models import PublishedPost , Account
from app import app, db

LINKEDIN_VERSION = "202604"

def publish_to_facebook(message , user_id):

    try:
        with app.app_context():
            account = Account.query.filter_by(user_id=user_id).first()
    except Exception as e:
        print(str(e))

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


def publish_to_linkedin(message, user_id):

    with app.app_context():

        account = Account.query.filter_by(
            user_id=user_id
        ).first()

        if not account:
            raise ValueError(
                "No account found for this user"
            )

        linkedin_access_token = (
            account.linkedin_access_token
        )

        author_urn = account.author_urn

        if not linkedin_access_token:
            raise ValueError(
                "LinkedIn access token is missing"
            )

        if not author_urn:
            raise ValueError(
                "LinkedIn author URN is missing"
            )

        url = "https://api.linkedin.com/rest/posts"

        headers = {
            "Authorization":
                f"Bearer {linkedin_access_token}",

            "Content-Type":
                "application/json",

            "X-Restli-Protocol-Version":
                "2.0.0",

            "LinkedIn-Version":
                LINKEDIN_VERSION
        }

        data = {
            "author": author_urn,

            "commentary": message,

            "visibility": "PUBLIC",

            "distribution": {
                "feedDistribution": "MAIN_FEED",
                "targetEntities": [],
                "thirdPartyDistributionChannels": []
            },

            "lifecycleState": "PUBLISHED",

            "isReshareDisabledByAuthor": False
        }

        try:

            print("Starting LinkedIn publishing...")
            print("Message length:", len(message))

            response = requests.post(
                url,
                headers=headers,
                json=data,
                timeout=60
            )

            print(
                "LinkedIn Status Code:",
                response.status_code
            )

            print(
                "LinkedIn Response:",
                response.text[:500]
            )

            response.raise_for_status()

            published_post = PublishedPost(
                user_id=user_id,
                platform="LinkedIn",
                post_content=message
            )

            db.session.add(published_post)
            db.session.commit()

            print(
                "LinkedIn post saved to database successfully!"
            )

            return {
                "status_code": response.status_code,
                "response": response.json(),
                "post_id": response.headers.get(
                    "x-restli-id"
                )
            }

        except Exception as e:

            db.session.rollback()

            print(
                "LINKEDIN ERROR:",
                repr(e)
            )

            raise

def publish_to_instagram(
    message,
    user_id,
    image_url
):

    with app.app_context():

        account = Account.query.filter_by(
            user_id=user_id
        ).first()

        if not account:
            raise ValueError(
                "No account found for this user"
            )

        instagram_business_id = (
            account.instagram_business_id
        )

        page_access_token = (
            account.page_access_token
        )

        if not instagram_business_id:
            raise ValueError(
                "Instagram Business ID is missing"
            )

        if not page_access_token:
            raise ValueError(
                "Instagram page access token is missing"
            )

        if not image_url:
            raise ValueError(
                "Instagram image URL is missing"
            )

        try:

            # ------------------------------------------------
            # Step 1: Create Instagram media container
            # ------------------------------------------------

            print(
                "Creating Instagram media container..."
            )

            container_url = (
                f"https://graph.facebook.com/v23.0/"
                f"{instagram_business_id}/media"
            )

            container_response = requests.post(
                container_url,
                data={
                    "image_url": image_url,
                    "caption": message,
                    "access_token": page_access_token
                },
                timeout=60
            )

            print(
                "Instagram container status:",
                container_response.status_code
            )

            print(
                "Instagram container response:",
                container_response.text[:500]
            )

            container_response.raise_for_status()

            container_data = (
                container_response.json()
            )

            creation_id = (
                container_data.get("id")
            )

            if not creation_id:
                raise ValueError(
                    "Instagram did not return a creation ID"
                )

            # ------------------------------------------------
            # Step 2: Publish the media container
            # ------------------------------------------------

            print(
                "Publishing Instagram media..."
            )

            publish_url = (
                f"https://graph.facebook.com/v23.0/"
                f"{instagram_business_id}/media_publish"
            )

            publish_response = requests.post(
                publish_url,
                data={
                    "creation_id": creation_id,
                    "access_token": page_access_token
                },
                timeout=60
            )

            print(
                "Instagram publish status:",
                publish_response.status_code
            )

            print(
                "Instagram publish response:",
                publish_response.text[:500]
            )

            publish_response.raise_for_status()

            # ------------------------------------------------
            # Step 3: Save successful publication
            # ------------------------------------------------

            published_post = PublishedPost(
                user_id=user_id,
                platform="Instagram",
                post_content=message
            )

            db.session.add(published_post)
            db.session.commit()

            print(
                "Instagram post saved to database successfully!"
            )

            return publish_response.json()

        except Exception as e:

            db.session.rollback()

            print(
                "INSTAGRAM ERROR:",
                repr(e)
            )

            raise