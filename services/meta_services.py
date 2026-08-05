import os
import secrets
import requests
from flask import session , request
from urllib.parse import urlencode

from app import db
from init_db.meta_accounts.models import MetaAccount

from datetime import datetime, timedelta, timezone,  UTC

APP_ID = os.getenv("META_APP_ID")
REDIRECT_URI = os.getenv("META_REDIRECT_URI")
APP_SECRET = os.getenv("META_APP_SECRET")


def meta_login():

    state = secrets.token_urlsafe(32)

    session["meta_oauth_state"] = state

    params = {
        "client_id": APP_ID,
        "redirect_uri": REDIRECT_URI,
        "response_type": "code",
        "state": state,
        "scope": ",".join([
            "pages_show_list",
            "business_management",
            "instagram_content_publish",
            "instagram_manage_comments",
            "pages_manage_posts",
            "pages_manage_engagement",
            "pages_read_engagement",
            "pages_read_user_content",
            "public_profile"
        ])
    }

    return (
        "https://www.facebook.com/v23.0/dialog/oauth?"
        + urlencode(params)
    )


def retrieve_meta_auth_code():

    auth_code = request.args.get("code")

    if not auth_code:
        raise ValueError("Authorization code missing.")

    returned_state = request.args.get("state")

    saved_state = session.get("meta_oauth_state")

    if returned_state != saved_state:
        raise ValueError("Invalid OAuth state.")

    return auth_code


def exchange_meta_token(auth_code):

    url = "https://graph.facebook.com/v23.0/oauth/access_token"

    params = {
        "client_id": APP_ID,
        "redirect_uri": REDIRECT_URI,
        "client_secret": APP_SECRET,
        "code": auth_code
    }

    response = requests.get(url, params=params)

    if response.status_code != 200:
        raise RuntimeError(response.text)

    data = response.json()

    return data["access_token"]



def get_long_lived_token(short_token):

    url = "https://graph.facebook.com/v23.0/oauth/access_token"

    params = {
        "grant_type": "fb_exchange_token",
        "client_id": APP_ID,
        "client_secret": APP_SECRET,
        "fb_exchange_token": short_token
    }

    response = requests.get(url, params=params)

    if response.status_code != 200:
        raise RuntimeError(response.text)

    data = response.json()

    print("Long-lived token response:", data)

    access_token = data["access_token"]

    expires_in = data.get("expires_in")

    if expires_in is None:
        # Business Login tokens may not include an expiry.
        expires_at = None
    else:
        expires_at = datetime.now(UTC) + timedelta(seconds=expires_in)

    return access_token, expires_at

def get_user_info(access_token):

    url = "https://graph.facebook.com/v23.0/me"

    params = {
        "fields": "id,name",
        "access_token": access_token
    }

    response = requests.get(url, params=params)

    if response.status_code != 200:
        raise RuntimeError(response.text)

    return response.json()

def get_pages(access_token):

    url = "https://graph.facebook.com/v23.0/me/accounts"

    params = {
        "access_token": access_token
    }

    response = requests.get(url, params=params)

    if response.status_code != 200:
        raise RuntimeError(response.text)

    return response.json()["data"]

def get_instagram_business(page_id, page_token):

    url = f"https://graph.facebook.com/v23.0/{page_id}"

    params = {
        "fields": "instagram_business_account",
        "access_token": page_token
    }

    response = requests.get(url, params=params)

    if response.status_code != 200:
        raise RuntimeError(response.text)

    return response.json().get("instagram_business_account")


def add_meta_to_database(account: MetaAccount):

    try:
        db.session.add(account)
        db.session.commit()

    except Exception:
        db.session.rollback()
        raise