import os
import requests
from dotenv import load_dotenv

load_dotenv()


class NotifyConfigurationError(Exception):
    pass


class NotifyEmailError(Exception):
    pass


def validate_notify_config():
    missing = []

    if not os.getenv("NOTIFY_API_KEY"):
        missing.append("NOTIFY_API_KEY")

    if not os.getenv("NOTIFY_APP_ID"):
        missing.append("NOTIFY_APP_ID")

    if not os.getenv("NOTIFY_BRAND_ID"):
        missing.append("NOTIFY_BRAND_ID")

    if not os.getenv("NOTIFY_EMAIL_ENDPOINT"):
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
    """Send an HTML email through ELVA Notify using its configured endpoint."""

    validate_notify_config()
    api_key = os.getenv("NOTIFY_API_KEY")
    headers = {
        "Content-Type": "application/json",
    }

    payload = {
        "appId": os.getenv("NOTIFY_APP_ID"),
        "apiKey": api_key,
        "brandId": os.getenv("NOTIFY_BRAND_ID"),
        "channel": "EMAIL",
        "to": [recipient],
        "subject": subject,
        "html": body,
    }

    try:

        response = requests.post(
            os.getenv("NOTIFY_EMAIL_ENDPOINT"),
            headers=headers,
            json=payload,
            timeout=60,
        )

        response.raise_for_status()
        try:
            return response.json()
        except ValueError:
            return {"status": "sent", "response": response.text}

    except requests.RequestException as exc:

        raise NotifyEmailError(
            f"Notify email request failed: {exc}"
        ) from exc
