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


def apply_company_logo(
    image_path: str | Path,
    logo_path: str | Path,
    top_margin_ratio: float = 0.035,
    right_margin_ratio: float = 0.035,
) -> str:
    """Composite an organization PNG logo into the top-right of an image."""
    import cv2
    import numpy as np

    image_path = str(image_path)
    background = cv2.imread(image_path, cv2.IMREAD_COLOR)
    logo = cv2.imread(str(logo_path), cv2.IMREAD_UNCHANGED)
    if background is None or logo is None:
        raise ValueError("The generated image or company logo could not be read.")

    if logo.ndim == 2:
        logo = cv2.cvtColor(logo, cv2.COLOR_GRAY2BGRA)
    elif logo.shape[2] == 3:
        alpha = np.full(logo.shape[:2], 255, dtype=np.uint8)
        logo = np.dstack((logo, alpha))
    elif logo.shape[2] != 4:
        raise ValueError("Company logo must be a PNG with RGB or RGBA channels.")

    image_height, image_width = background.shape[:2]
    logo_height, logo_width = logo.shape[:2]
    max_logo_width = max(1, round(image_width * 0.20))
    max_logo_height = max(1, round(image_height * 0.20))
    scale = min(max_logo_width / logo_width, max_logo_height / logo_height)
    resized_width = max(1, round(logo_width * scale))
    resized_height = max(1, round(logo_height * scale))
    interpolation = cv2.INTER_AREA if scale < 1 else cv2.INTER_CUBIC
    logo = cv2.resize(logo, (resized_width, resized_height), interpolation=interpolation)

    # A restrained contrast plate keeps both light and dark marks legible.
    visible = logo[:, :, 3] > 16
    if np.any(visible):
        bgr = logo[:, :, :3][visible].astype(np.float32)
        luminance = float(np.mean(0.114 * bgr[:, 0] + 0.587 * bgr[:, 1] + 0.299 * bgr[:, 2]))
    else:
        luminance = 0
    plate_color = np.array([32, 42, 64] if luminance > 178 else [250, 250, 250], dtype=np.float32)
    padding = max(6, round(min(image_width, image_height) * 0.012))
    plate_width = resized_width + padding * 2
    plate_height = resized_height + padding * 2
    top_margin = max(4, round(image_height * top_margin_ratio))
    right_margin = max(4, round(image_width * right_margin_ratio))
    plate_x = max(0, image_width - right_margin - plate_width)
    plate_y = min(top_margin, max(0, image_height - plate_height))

    plate_roi = background[plate_y:plate_y + plate_height, plate_x:plate_x + plate_width]
    plate_alpha = 0.84
    plate_roi[:] = np.clip(
        plate_roi.astype(np.float32) * (1 - plate_alpha) + plate_color * plate_alpha,
        0, 255
    ).astype(np.uint8)
    cv2.rectangle(
        background,
        (plate_x, plate_y),
        (plate_x + plate_width - 1, plate_y + plate_height - 1),
        (222, 226, 233),
        1,
        lineType=cv2.LINE_AA,
    )

    logo_x = plate_x + padding
    logo_y = plate_y + padding
    logo_roi = background[logo_y:logo_y + resized_height, logo_x:logo_x + resized_width]
    alpha = logo[:, :, 3:4].astype(np.float32) / 255.0
    logo_roi[:] = np.clip(
        logo[:, :, :3].astype(np.float32) * alpha + logo_roi.astype(np.float32) * (1 - alpha),
        0, 255
    ).astype(np.uint8)

    if not cv2.imwrite(image_path, background):
        raise OSError("Unable to save the branded image.")
    return image_path
