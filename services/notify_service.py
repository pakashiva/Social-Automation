import os
import requests


NOTIFY_API_KEY = os.getenv("NOTIFY_API_KEY")
NOTIFY_APP_ID = os.getenv("NOTIFY_APP_ID")
NOTIFY_BRAND_ID = os.getenv("NOTIFY_BRAND_ID")
NOTIFY_EMAIL_ENDPOINT = os.getenv("NOTIFY_EMAIL_ENDPOINT")


class NotifyConfigurationError(Exception):
    pass


class NotifyEmailError(Exception):
    pass


def validate_notify_config():
    missing = []

    if not NOTIFY_API_KEY:
        missing.append("NOTIFY_API_KEY")

    if not NOTIFY_APP_ID:
        missing.append("NOTIFY_APP_ID")

    if not NOTIFY_BRAND_ID:
        missing.append("NOTIFY_BRAND_ID")

    if not NOTIFY_EMAIL_ENDPOINT:
        missing.append("NOTIFY_EMAIL_ENDPOINT")

    if missing:
        raise NotifyConfigurationError(
            "Missing Notify configuration: "
            + ", ".join(missing)
        )


def send_email(
    *,
    recipient,
    subject,
    body,
):
    """
    Send an email through ELVA Notify.

    The exact endpoint, headers and payload fields must be
    filled according to the Notify integration documentation
    once API access/documentation is received.
    """

    validate_notify_config()

    headers = {
        # Replace/add these according to the official
        # Notify integration documentation.
        "Content-Type": "application/json",
        "Authorization": f"Bearer {NOTIFY_API_KEY}",
    }

    payload = {
        "appId": NOTIFY_APP_ID,
        "brandId": NOTIFY_BRAND_ID,

        "to": recipient,

        "subject": subject,

        "body": body,
    }

    try:

        response = requests.post(
            NOTIFY_EMAIL_ENDPOINT,
            headers=headers,
            json=payload,
            timeout=30,
        )

        response.raise_for_status()

        return response.json()

    except requests.RequestException as exc:

        raise NotifyEmailError(
            f"Notify email request failed: {exc}"
        ) from exc