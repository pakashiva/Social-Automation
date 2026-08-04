import secrets, os, requests
from flask import session ,request
from urllib.parse import urlencode
from datetime import datetime, timedelta , UTC
from dotenv import load_dotenv
from init_db.linkedin_accounts.models import LinkedInAccount
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

def add_to_database(account : LinkedInAccount):
    try:
        db.session.add(account)
        db.session.commit()
    except Exception:
        db.session.rollback()
        raise