"""
Image generation functions.

Generates images using Hugging Face Inference API
and stores them in the scheduled uploads directory.
"""

import os
import uuid
from pathlib import Path
from dotenv import load_dotenv
from huggingface_hub import InferenceClient


load_dotenv()


# ---------------------------------------------------------
# Configuration
# ---------------------------------------------------------

BASE_DIR = Path(__file__).resolve().parent.parent.parent

SCHEDULED_UPLOAD_FOLDER = os.path.join(
    BASE_DIR,
    "static",
    "scheduled_uploads"
)


os.makedirs(
    SCHEDULED_UPLOAD_FOLDER,
    exist_ok=True
)


# ---------------------------------------------------------
# Hugging Face Client
# ---------------------------------------------------------

client = InferenceClient(
    provider="fal-ai",
    api_key=os.environ["HF_TOKEN"],
)


# ---------------------------------------------------------
# Image Generator
# ---------------------------------------------------------

def generate_image(
    prompt: str,
    user_id
) -> dict:
    """
    Generate an image using Hugging Face and save it locally.

    Parameters
    ----------
    prompt:
        Image generation prompt.

    user_id:
        ID of the user for whom the image is generated.

    Returns
    -------
    dict
        Information about the generated image.
    """

    if not prompt or not prompt.strip():
        raise ValueError(
            "Image generation prompt is required."
        )

    if not user_id:
        raise ValueError(
            "User ID is required."
        )

    prompt = prompt.strip()

    # -----------------------------------------------------
    # Generate image
    # -----------------------------------------------------

    image = client.text_to_image(
        prompt=prompt,
        model="stabilityai/stable-diffusion-xl-base-1.0",
    )

    # -----------------------------------------------------
    # Generate unique filename
    # -----------------------------------------------------

    filename = (
        f"{uuid.uuid4().hex}.png"
    )

    # -----------------------------------------------------
    # Full local file path
    # -----------------------------------------------------

    file_path = os.path.join(
        SCHEDULED_UPLOAD_FOLDER,
        filename
    )

    # -----------------------------------------------------
    # Save image
    # -----------------------------------------------------

    image.save(file_path)

    print(
        "IMAGE GENERATED:",
        flush=True
    )

    # -----------------------------------------------------
    # Return image information
    # -----------------------------------------------------

    return {
        "user_id": user_id,
        "filename": filename,
        "file_path": str(file_path)
    }