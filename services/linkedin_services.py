import secrets, os, requests
from flask import session ,request
from urllib.parse import urlencode
from datetime import datetime, timedelta , UTC
from dotenv import load_dotenv
from initialize_database.models import Account
from app import db

load_dotenv()

CLIENT_ID = os.getenv("LINKEDIN_CLIENT_ID")
CLIENT_SECRET = os.getenv("LINKEDIN_CLIENT_SECRET")
REDIRECT_URI = os.getenv("LINKEDIN_REDIRECT_URI")


def logging_in():
    state = secrets.token_urlsafe(32)

    session["oauth_state"] = state

    params = {
    "response_type": "code",
    "client_id": CLIENT_ID,
    "redirect_uri": REDIRECT_URI,
    "state": state,

    # w_prganization_social is used for posting on behalf of linkedIn.
    # w_member_social is for member  of the company.
    
    "scope": "openid profile email w_member_social"
    }

    linkedin_url = (
        "https://www.linkedin.com/oauth/v2/authorization?"
        + urlencode(params)
    )

    return linkedin_url

# Retrieving Auth code
def retrive_auth_code():
    Auth_code = request.args.get("code")

    if not Auth_code:
        raise ValueError(f"Authorization code missing {Auth_code}")
    
    returned_state = request.args.get("state")

    saved_state = session["oauth_state"]

    if Auth_code:
        print("Authentication code recived! , Auth code:" , Auth_code)

    if saved_state != returned_state:
        print("Returned State:", returned_state)
        print("Saved State:", saved_state)

        raise ValueError("Invalid OAuth state")
    
    return Auth_code

# Token Exchange
def token_exchange(Auth_code):
    token_url = "https://www.linkedin.com/oauth/v2/accessToken"

    payload = {
    "grant_type": "authorization_code",
    "code": Auth_code,
    "client_id": CLIENT_ID,
    "client_secret": CLIENT_SECRET,
    "redirect_uri": REDIRECT_URI,
    }

    try:
        response = requests.post(token_url, data=payload)
    except requests.RequestException as e:
        raise RuntimeError("Unable to contact LinkedIn") from e

    if response.status_code != 200:
        raise RuntimeError(f"LinkedIn token exchange failed: {response.text}")

    try:
        token_data = response.json()
    except ValueError:
        raise RuntimeError("LinkedIn returned invalid JSON")

    #priting data to verify.
    
    print("Token data: \n " , token_data)

    access_token = token_data["access_token"]
    expires_in = token_data["expires_in"]


    if not access_token or expires_in is None:
        raise RuntimeError("Token response incomplete")

    expires_at = datetime.now(UTC)  + timedelta(seconds=expires_in)

    return access_token , expires_at

def get_author_urn(access_token):
    url = "https://api.linkedin.com/v2/userinfo"

    headers = {
        "Authorization": f"Bearer {access_token}"
    }

    response = requests.get(url, headers=headers)

    if response.status_code != 200:
        raise RuntimeError(
            f"Failed to fetch user info: {response.status_code} {response.text}"
        )

    data = response.json()

    # 'sub' contains the member's LinkedIn ID
    member_id = data.get("sub")

    if not member_id:
        raise RuntimeError("LinkedIn user ID not found.")

    return f"urn:li:person:{member_id}"

def fetch_company_urn(access_token):
    url = (
        "https://api.linkedin.com/v2/organizationalEntityAcls"
        "?q=roleAssignee"
        "&role=ADMINISTRATOR"
        "&state=APPROVED"
    )

    headers = {
        "Authorization": f"Bearer {access_token}",
        "LinkedIn-Version": "202507",
        "X-Restli-Protocol-Version": "2.0.0"
    }

    response = requests.get(url, headers=headers)

    if response.status_code != 200:
        raise RuntimeError(response.text)

    data = response.json()

    if not data.get("elements"):
        raise RuntimeError("User is not an administrator of any LinkedIn organization.")

    return data["elements"][0]["organizationalTarget"]

def get_published_posts(access_token, author_urn, count=20):
    url = (
        "https://api.linkedin.com/rest/posts"
        f"?q=author&author={author_urn}&count={count}&sortBy=LAST_MODIFIED"
    )

    headers = {
        "Authorization": f"Bearer {access_token}",
        "LinkedIn-Version": "202507",
        "X-Restli-Protocol-Version": "2.0.0"
    }

    response = requests.get(url, headers=headers)

    if response.status_code != 200:
        raise RuntimeError(
            f"Failed to fetch posts: {response.status_code}\n{response.text}"
        )

    return response.json()
