from app import db , app
import json
import time
import requests
from initialize_database.models import PublishedPost , Account
from app import app, db

LINKEDIN_VERSION = "202604"

def publish_to_facebook(message, user_id, image_url=None, image_urls=None):
    image_urls = image_urls or ([image_url] if image_url else [])
    with app.app_context():
        account = Account.query.filter_by(user_id=user_id).first()
        if not account:
            raise ValueError("No Facebook account found")
        page_access_token = account.page_access_token
        page_id = account.page_id

    if not page_id:
        raise ValueError("Facebook PAGE_ID is missing")
    if not page_access_token:
        raise ValueError("Facebook PAGE_ACCESS_TOKEN is missing")

    if not image_urls:
        response = requests.post(
            f"https://graph.facebook.com/v23.0/{page_id}/feed",
            data={"message": message, "access_token": page_access_token},
            timeout=60
        )
        response.raise_for_status()
        return response.json()

    if len(image_urls) == 1:
        response = requests.post(
            f"https://graph.facebook.com/v23.0/{page_id}/photos",
            data={
                "url": image_urls[0],
                "caption": message,
                "access_token": page_access_token,
            },
            timeout=60
        )
        response.raise_for_status()
        return response.json()

    photo_ids = []
    for image_url in image_urls:
        upload_response = requests.post(
            f"https://graph.facebook.com/v23.0/{page_id}/photos",
            data={
                "url": image_url,
                "published": "false",
                "access_token": page_access_token,
            },
            timeout=60
        )
        upload_response.raise_for_status()
        photo_id = upload_response.json().get("id")
        if not photo_id:
            raise RuntimeError("Facebook did not return an uploaded photo ID.")
        photo_ids.append(photo_id)

    post_data = {"message": message, "access_token": page_access_token}
    for index, photo_id in enumerate(photo_ids):
        post_data[f"attached_media[{index}]"] = json.dumps({"media_fbid": photo_id})

    response = requests.post(
        f"https://graph.facebook.com/v23.0/{page_id}/feed",
        data=post_data,
        timeout=60
    )
    response.raise_for_status()
    return response.json()


def _upload_linkedin_image(image_path, access_token, author_urn):
    headers = {
        "Authorization": f"Bearer {access_token}",
        "LinkedIn-Version": LINKEDIN_VERSION,
        "X-Restli-Protocol-Version": "2.0.0",
        "Content-Type": "application/json",
    }
    initialize_response = requests.post(
        "https://api.linkedin.com/rest/images?action=initializeUpload",
        headers=headers,
        json={"initializeUploadRequest": {"owner": author_urn}},
        timeout=60
    )
    initialize_response.raise_for_status()
    upload_details = initialize_response.json().get("value", {})
    upload_url = upload_details.get("uploadUrl")
    image_urn = upload_details.get("image")
    if not upload_url or not image_urn:
        raise RuntimeError("LinkedIn did not return image upload details.")

    with open(image_path, "rb") as image_file:
        upload_response = requests.put(
            upload_url,
            headers={"Authorization": f"Bearer {access_token}"},
            data=image_file,
            timeout=120
        )
    upload_response.raise_for_status()
    return image_urn


def publish_to_linkedin(message, user_id, image_paths=None):
    image_paths = image_paths or []

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

        image_urns = [
            _upload_linkedin_image(path, linkedin_access_token, author_urn)
            for path in image_paths
        ]

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

        if len(image_urns) == 1:
            data["content"] = {"media": {"id": image_urns[0]}}
        elif len(image_urns) > 1:
            data["content"] = {
                "multiImage": {
                    "images": [{"id": image_urn} for image_urn in image_urns]
                }
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
                "response": response.json() if response.content else {},
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

def _wait_for_instagram_container(container_id, page_access_token):
    status_url = f"https://graph.facebook.com/v23.0/{container_id}"
    for _ in range(30):
        response = requests.get(
            status_url,
            params={
                "fields": "status_code",
                "access_token": page_access_token,
            },
            timeout=30
        )
        response.raise_for_status()
        status = response.json().get("status_code")
        if status == "FINISHED":
            return
        if status in {"ERROR", "EXPIRED"}:
            raise RuntimeError(f"Instagram image processing failed ({status}).")
        time.sleep(2)
    raise RuntimeError("Instagram is still processing the selected photos. Please try again shortly.")


def publish_to_instagram(message, user_id, image_url=None, image_urls=None):
    image_urls = image_urls or ([image_url] if image_url else [])

    with app.app_context():
        account = Account.query.filter_by(user_id=user_id).first()
        if not account:
            raise ValueError("No Instagram account found for this user")

        instagram_business_id = account.instagram_business_id
        page_access_token = account.page_access_token

        if not instagram_business_id:
            raise ValueError("Instagram Business ID is missing")
        if not page_access_token:
            raise ValueError("Instagram page access token is missing")
        if not image_urls:
            raise ValueError("Instagram publishing requires an image")

        try:
            media_url = f"https://graph.facebook.com/v23.0/{instagram_business_id}/media"
            publish_url = f"https://graph.facebook.com/v23.0/{instagram_business_id}/media_publish"

            if len(image_urls) == 1:
                container_response = requests.post(
                    media_url,
                    data={
                        "image_url": image_urls[0],
                        "caption": message,
                        "access_token": page_access_token,
                    },
                    timeout=60
                )
                container_response.raise_for_status()
                creation_id = container_response.json().get("id")
            else:
                child_ids = []
                for image_url in image_urls:
                    child_response = requests.post(
                        media_url,
                        data={
                            "image_url": image_url,
                            "is_carousel_item": "true",
                            "access_token": page_access_token,
                        },
                        timeout=60
                    )
                    child_response.raise_for_status()
                    child_id = child_response.json().get("id")
                    if not child_id:
                        raise RuntimeError("Instagram did not return a photo container ID.")
                    child_ids.append(child_id)
                    _wait_for_instagram_container(child_id, page_access_token)

                carousel_response = requests.post(
                    media_url,
                    data={
                        "media_type": "CAROUSEL",
                        "children": ",".join(child_ids),
                        "caption": message,
                        "access_token": page_access_token,
                    },
                    timeout=60
                )
                carousel_response.raise_for_status()
                creation_id = carousel_response.json().get("id")
                if creation_id:
                    _wait_for_instagram_container(creation_id, page_access_token)

            if not creation_id:
                raise RuntimeError("Instagram did not return a post container ID.")

            publish_response = requests.post(
                publish_url,
                data={
                    "creation_id": creation_id,
                    "access_token": page_access_token,
                },
                timeout=60
            )
            publish_response.raise_for_status()

            published_post = PublishedPost(
                user_id=user_id,
                platform="Instagram",
                post_content=message
            )
            db.session.add(published_post)
            db.session.commit()
            return publish_response.json()

        except Exception:
            db.session.rollback()
            raise
