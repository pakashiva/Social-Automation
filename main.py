import os ,json ,traceback
import shutil
from zoneinfo import ZoneInfo
from app import app, db, jwt
from uuid import uuid4 
import uuid
from pathlib import Path
from urllib.parse import urlparse
from ruamel.yaml import YAML
from dotenv import load_dotenv
from flask_sqlalchemy import SQLAlchemy
from initialize_database.models import Account, PublishedPost, User, CompanyInfo , ContentJob , RecurringContent
from datetime import UTC, datetime, timedelta
from cron_converter.cron_conversion import convert_to_cron
from werkzeug.security import check_password_hash, generate_password_hash
from werkzeug.utils import secure_filename
from agents.image_prompt_generator.functions import generate_image_prompt
from flask import (
    Response , 
    flash,
    get_flashed_messages,
    make_response,
    redirect,
    render_template,
    request,
    url_for,
    jsonify,
    stream_with_context
)
from flask_jwt_extended import (
    create_access_token,
    get_jwt_identity,
    jwt_required,
    verify_jwt_in_request,
    unset_jwt_cookies
)

from services.linkedin_services import (
    get_author_urn,
    logging_in,
    retrive_auth_code,
    token_exchange,
)

from services.meta_services import (
    exchange_meta_token,
    get_long_lived_token,
    get_pages,
    get_user_info,
    meta_login,
    retrieve_meta_auth_code,
)


load_dotenv()

#development purpose only, recommended to use cloud storage sevices for files
UPLOAD_FOLDER = Path("uploads")
UPLOAD_FOLDER.mkdir(exist_ok=True)

# jwt error handler.
@jwt.expired_token_loader
def expired_token(jwt_header, jwt_payload):
    flash("Your session has expired. Please log in again.", "warning")
    return redirect(url_for("login"))


@jwt.invalid_token_loader
def invalid_token(reason):
    flash("Invalid session. Please log in again.", "danger")
    return redirect(url_for("login"))


@jwt.unauthorized_loader
def missing_token(reason):
    flash("Please log in to continue.", "warning")
    return redirect(url_for("login"))


@jwt.revoked_token_loader
def revoked_token(jwt_header, jwt_payload):
    flash("Your session is no longer valid. Please log in again.", "warning")
    return redirect(url_for("login"))


# checks whether the current request has a valid JWT login token
@app.context_processor
def inject_auth_status():

    try:
        verify_jwt_in_request(optional=True)
        user_id = get_jwt_identity()
        return {
            "logged_in": user_id is not None
        }

    except Exception:

        return {
            "logged_in": False
        }


@app.after_request
def refresh_page_session(response):
    """Renew the 15-day JWT when an authenticated HTML page is visited."""
    if request.method != "GET" or response.mimetype != "text/html" or request.path.startswith("/static/"):
        return response
    try:
        verify_jwt_in_request(optional=True)
        user_id = get_jwt_identity()
        if user_id:
            refreshed = create_access_token(identity=user_id, expires_delta=timedelta(days=15))
            response.set_cookie(
                "access_token", refreshed, max_age=15 * 24 * 60 * 60,
                httponly=True, secure=False, samesite="Lax"
            )
    except Exception:
        pass
    return response

UPLOAD_FOLDER = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "static",
    "uploads"
)

ALLOWED_IMAGE_EXTENSIONS = {
    "png",
    "jpg",
    "jpeg",
    "webp"
}

os.makedirs(UPLOAD_FOLDER, exist_ok=True)

def render_page(template_name, title, active_page):
    return render_template(template_name, title=title, active_page=active_page)

# Page routing for the Flask application

@app.route("/")
def home():
    return render_page("home.html", "Home — ELVA SocialAI", "home")


@app.route("/about")
def about():
    return render_page("about.html", "About Us — ELVA SocialAI", "about")


@app.route("/oauth")
def oauth():
    try:
        verify_jwt_in_request(optional=True)
        user_id = get_jwt_identity()
    except Exception:
        user_id = None

    account = Account.query.filter_by(user_id=user_id).first() if user_id else None

    def expiry_text(expires_at):
        if not expires_at:
            return "Expiry information is unavailable. Reconnect to refresh it."
        if expires_at.tzinfo is None:
            expires_at = expires_at.replace(tzinfo=UTC)
        seconds_left = (expires_at - datetime.now(UTC)).total_seconds()
        if seconds_left <= 0:
            return "Expired. Reconnect to publish."
        days_left = max(1, int((seconds_left + 86399) // 86400))
        return f"Reconnect in {days_left} day{'s' if days_left != 1 else ''}."

    def is_expired(expires_at):
        if not expires_at:
            return False
        if expires_at.tzinfo is None:
            expires_at = expires_at.replace(tzinfo=UTC)
        return expires_at <= datetime.now(UTC)

    account_status = {
        "logged_in": user_id is not None,
        "linkedin_connected": bool(account and account.linkedin_access_token and account.author_urn),
        "linkedin_expiry": expiry_text(account.linkedin_token_expires_at) if account and account.linkedin_access_token else "",
        "linkedin_expired": bool(account and is_expired(account.linkedin_token_expires_at)),
        "meta_expiry": expiry_text(account.meta_token_expires_at) if account and account.page_access_token else "",
        "meta_expired": bool(account and is_expired(account.meta_token_expires_at)),
        "facebook_connected": bool(account and account.page_access_token and account.page_id),
        "facebook_page_name": account.page_name if account and account.page_id else None,
        "instagram_connected": bool(account and account.page_access_token and account.instagram_business_id),
    }

    return render_template(
        "oauth.html",
        title="Social Accounts — ELVA SocialAI",
        active_page="oauth",
        account_status=account_status,
    )


@app.route("/company")
def company():
    try:
        verify_jwt_in_request(optional=True)
        user_id = get_jwt_identity()
    except Exception:
        user_id = None
    company_info = CompanyInfo.query.filter_by(user_id=user_id).first() if user_id else None
    return render_template(
        "company.html",
        title="Company Data — ELVA SocialAI",
        active_page="company",
        company=company_info,
    )


@app.route("/schedule")
def schedule():
    try:
        verify_jwt_in_request(optional=True)
        user_id = get_jwt_identity()
    except Exception:
        user_id = None
    company = CompanyInfo.query.filter_by(user_id=user_id).first() if user_id else None
    return render_template("schedule.html", title="Schedule Content — ELVA SocialAI", active_page="schedule", company=company)

@app.route("/create_content")
@jwt_required()
def create_content():
    return render_page(
        "create_content.html",
        "Create Content — ELVA SocialAI",
        "create_content"
    )

@app.route("/content-calendar", methods=["GET"])
@jwt_required()
def content_calendar():

    return render_page(
        "content_calendar.html",
        "Content Calendar — ELVA SocialAI",
        "content_calendar"
    )

@app.route("/posts")
@jwt_required()
def posts():

    user_id = get_jwt_identity()

    posts = PublishedPost.query.filter_by(
        user_id=user_id
    ).order_by(
        PublishedPost.posted_at.desc()
    ).all()

    pending_posts = []
    scheduled_jobs = ContentJob.query.filter_by(
        user_id=user_id, status="scheduled"
    ).all()
    scheduled_recurring = RecurringContent.query.filter_by(
        user_id=user_id, status="scheduled"
    ).all()

    def add_pending_post(kind, post, images):
        today = datetime.now(UTC).date()
        used = post.regeneration_count or 0
        if post.regeneration_date != today:
            used = 0
        scheduled_at = post.scheduled_at
        if scheduled_at and scheduled_at.tzinfo is None:
            scheduled_at = scheduled_at.replace(tzinfo=UTC)
        pending_posts.append({
            "kind": kind,
            "id": post.id,
            "platform": post.platform,
            "status": post.status,
            "approval_status": post.approval_status,
            "hitl_required": post.hitl_required,
            "post_content": post.post_content or "",
            "scheduled_at": post.scheduled_at.strftime("%b %d, %Y at %I:%M %p") if post.scheduled_at else "",
            "_sort_key": scheduled_at.timestamp() if scheduled_at else float("inf"),
            "images": images,
            "regenerations_left": max(0, 3 - used),
            "has_saved_generation_inputs": bool(
                (post.generation_context if kind == "recurring" else post.generation_source)
            ),
        })

    for post in scheduled_jobs:
        add_pending_post("content_job", post, post.images or [])
    for post in scheduled_recurring:
        images = post.images or []
        if not images and post.image_filename:
            images = [{
                "image_url": url_for("static", filename=f"scheduled_uploads/{post.image_filename}", _external=True),
                "filename": post.image_filename,
            }]
        add_pending_post("recurring", post, images)
    pending_posts.sort(key=lambda item: item.pop("_sort_key"))

    return render_template(
        "posts.html", title="Posts — ELVA SocialAI", active_page="posts",
        posts=posts, pending_posts=pending_posts
    )

@app.route("/login", methods=["GET", "POST"])
def login():

    if request.method == "POST":

        email = request.form.get("email", "").strip().lower()

        password = request.form.get("password", "")

        user = User.query.filter_by(
            email=email
        ).first()

        if not user:

            flash("Invalid credentials." , "error")
            return redirect(url_for("login"))

        if not check_password_hash(
            user.password,
            password
        ):

            flash("Invalid credentials." , "error")

            return redirect(url_for("login"))

        print("GENERATING ACCESS TOKEN" , flush=True)

        access_token = create_access_token(
            identity=user.user_id,
            expires_delta=timedelta(days=15)
        )

        response = make_response(
            redirect(url_for("home"))
        )

        print("SENDING COOKIE" , flush=True)

        response.set_cookie(
            "access_token",
            access_token,
            max_age=15 * 24 * 60 * 60,
            httponly=True,
            secure=False,      # True in production (HTTPS)
            samesite="Lax"
        )
        flash("Logged in successfully." , "success")
        print("RESPONSE SENT", flush=True)

        return response

    return render_page(
        "login.html",
        "Login — ELVA SocialAI",
        "login"
    )


@app.route("/signup", methods=["GET", "POST"])
def signup():

    if request.method == "POST":

        email = request.form.get("email", "").strip().lower()
        password = request.form.get("password", "")

        if not email or not password:

            flash("Please fill all fields." , "error")
            return redirect(url_for("signup"))

        existing = User.query.filter_by(
            email=email
        ).first()

        if existing:

            flash("Email already exists." , "error")
            return redirect(url_for("signup"))

        user = User(
            email=email,
            password=generate_password_hash(password)
        )

        db.session.add(user)
        db.session.commit()
        flash("Account created successfully." , "success")
        return redirect(url_for("login"))

    return render_page(
        "signup.html",
        "Sign Up — ELVA SocialAI",
        "signup"
    )

# Saves schedule data from schedule.html

@app.route("/schedule_post", methods=["POST"])
@jwt_required()
def schedule_post():

    user_id = get_jwt_identity()

    input_text = request.form.get("schedule_task")
    timezone = request.form.get("timezone")

    platforms = request.form.getlist("platforms")
    notify_check = request.form.get("notify_check") == "true"
    notify_hours_before = request.form.get("notify_hours_before", type=int)
    unapproved_action = request.form.get("unapproved_action", "")

    if not input_text:
        return "Schedule instruction is required.", 400

    if not platforms:
        return "Please select at least one platform.", 400

    if notify_check and notify_hours_before not in {1, 2, 4, 6, 8}:
        return "Choose a notification lead time when approval is enabled.", 400
    if notify_check and unapproved_action not in {"publish", "skip"}:
        return "Choose what happens when a post is not approved.", 400

    cron_expression = convert_to_cron(input_text)

    company = CompanyInfo.query.filter_by(
        user_id=user_id
    ).first()

    if not company:

        company = CompanyInfo(
            user_id=user_id,
            scheduled_time=cron_expression,
            timezone=timezone,
            platforms=platforms,
            notify_check=notify_check,
            notify_hours_before=notify_hours_before if notify_check else None,
            publish_if_unapproved=(unapproved_action == "publish") if notify_check else True
        )

        db.session.add(company)

    else:

        company.scheduled_time = cron_expression
        company.timezone = timezone
        company.platforms = platforms
        company.notify_check = notify_check
        company.notify_hours_before = notify_hours_before if notify_check else None
        company.publish_if_unapproved = (unapproved_action == "publish") if notify_check else True

    db.session.commit()

    flash(
        "Schedule updated successfully",
        "success"
    )

    return redirect(url_for("schedule"))

# redirects to linkedIn page for user to login with linkedIn credentials

@app.route('/connect_linkedIn')
@jwt_required()
def connect():
    linkedin_url = logging_in()
    return redirect(linkedin_url)  # callback route will be called after successful login


# route to retrieve Auth code, token exchange , Saving in Database
@app.route('/callback')
@jwt_required()
def callback():

    user_id = get_jwt_identity()
    try:
        auth_code = retrive_auth_code()
    except ValueError as e:
        return str(e), 401
    
    try:
        access_token, expires_at = token_exchange(auth_code)
    except RuntimeError as e:
        return str(e), 500

    try:
        author_urn = get_author_urn(access_token)
    except RuntimeError as e:
        return str(e), 500

    account = Account.query.filter_by(user_id=user_id).first()

    if account:
        account.linkedin_access_token = access_token
        account.author_urn = author_urn
        account.linkedin_token_expires_at = expires_at
    else:
        account = Account(
            user_id=user_id,
            linkedin_access_token=access_token,
            author_urn=author_urn,
            linkedin_token_expires_at=expires_at
        )
        db.session.add(account)

    db.session.commit()

    flash("LinkedIn Account connected Successfully" , "Success")

    return redirect(url_for('oauth'))

# redirects to Facebook page for user to login with Facebook credentials
@app.route("/connect_meta")
@jwt_required()
def connect_meta():

    meta_url = meta_login()

    return redirect(meta_url)

# route to retrieve Auth code, token exchange , Saving in Database for meta
@app.route("/meta_callback")
@jwt_required()
def meta_callback():

    user_id = get_jwt_identity()

    # Step 1: Retrieve authorization code
    try:
        auth_code = retrieve_meta_auth_code()
    except ValueError as e:
        return str(e), 401

    # Step 2: Exchange code for a short-lived user token
    try:
        short_token = exchange_meta_token(auth_code)
    except RuntimeError as e:
        return str(e), 500

    # Step 3: Convert to a long-lived user token
    try:
        long_token, meta_expires_at = get_long_lived_token(short_token)
    except RuntimeError as e:
        return str(e), 500

    # Step 4: Retrieve Facebook user details (optional, useful for future)
    try:
        user = get_user_info(long_token)
    except RuntimeError as e:
        return str(e), 500

    # Step 5: Retrieve all managed Pages
    try:
        pages = get_pages(long_token)
    except RuntimeError as e:
        return str(e), 500

    if not pages:
        return "No Facebook Pages found.", 404

    # Prefer a Page that already has a linked Instagram Business account.
    # The current connection flow stores one Page per user.
    page = next(
        (candidate for candidate in pages if candidate.get("instagram_business_account")),
        pages[0]
    )

    page_id = page["id"]
    page_name = page["name"]
    page_token = page["access_token"]

    # Step 6: Retrieve the linked Instagram Business Account
    instagram = page.get("instagram_business_account") or {}

    print(
        "INSTAGRAM DATA FROM META:",
        instagram
    )

    instagram_id = instagram.get("id")

    print(
        "INSTAGRAM BUSINESS ID:",
        instagram_id,
        flush=True
    )

    account = Account.query.filter_by(user_id=user_id).first()

    if account:
        account.page_name = page_name
        account.page_id = page_id
        account.page_access_token = page_token
        account.instagram_business_id = instagram_id
        account.meta_token_expires_at = meta_expires_at
    else:
        account = Account(
            user_id=user_id,
            page_name=page_name,
            page_id=page_id,
            page_access_token=page_token,
            instagram_business_id=instagram_id,
            meta_token_expires_at=meta_expires_at
        )
        db.session.add(account)

    db.session.commit()
    if instagram_id:
        flash("Facebook Page and Instagram account connected successfully.", "success")
    else:
        flash("Facebook Page connected. No linked Instagram Business account was found.", "success")
    
    return redirect(url_for('oauth'))

# Saves company data from the company.html
@app.route('/save_company_data' , methods = ['POST'])
@jwt_required()
def save_company_info():
    user_id = get_jwt_identity()
    company = CompanyInfo.query.filter_by(user_id=user_id).first()
    brand_context = (request.form.get("brand_context") or "").strip()
    if not brand_context and company:
        brand_context = company.brand_context or ""
    if not brand_context:
        flash("Please enter brand context.", "error")
        return redirect(url_for("company"))

    pdf = request.files.get("strategy_pdf")
    has_pdf = bool(pdf and pdf.filename)
    if has_pdf and not pdf.filename.lower().endswith(".pdf"):
        flash("Only PDF files are allowed.", "error")
        return redirect(url_for("company"))
    if not has_pdf and not (company and company.content_strategy_path):
        flash("Please upload a PDF content strategy.", "error")
        return redirect(url_for("company"))

    logo_file = request.files.get("company_logo")
    logo_bytes = None
    if logo_file and logo_file.filename:
        logo_bytes = logo_file.stream.read((5 * 1024 * 1024) + 1)
        if len(logo_bytes) > 5 * 1024 * 1024:
            flash("The company logo must be 5 MB or smaller.", "error")
            return redirect(url_for("company"))
        if not logo_file.filename.lower().endswith(".png") or not logo_bytes.startswith(b"\x89PNG\r\n\x1a\n"):
            flash("Upload the company logo as a valid PNG file.", "error")
            return redirect(url_for("company"))
        if len(logo_bytes) < 24:
            flash("The PNG logo file is incomplete.", "error")
            return redirect(url_for("company"))
        logo_width = int.from_bytes(logo_bytes[16:20], "big")
        logo_height = int.from_bytes(logo_bytes[20:24], "big")
        if not logo_width or not logo_height or logo_width * logo_height > 25_000_000:
            flash("The logo dimensions are invalid or too large.", "error")
            return redirect(url_for("company"))
        try:
            import cv2
            import numpy as np
            decoded_logo = cv2.imdecode(np.frombuffer(logo_bytes, dtype=np.uint8), cv2.IMREAD_UNCHANGED)
        except Exception:
            decoded_logo = None
        if decoded_logo is None or decoded_logo.ndim not in {2, 3} or (decoded_logo.ndim == 3 and decoded_logo.shape[2] not in {3, 4}):
            flash("The PNG logo could not be decoded. Please choose another file.", "error")
            return redirect(url_for("company"))

    pdf_path = None
    strategy_json = None
    created_paths = []
    if has_pdf:
        user_folder = Path(UPLOAD_FOLDER) / user_id
        user_folder.mkdir(parents=True, exist_ok=True)
        pdf_path = user_folder / f"{uuid4().hex}.pdf"
        pdf.save(pdf_path)
        created_paths.append(pdf_path)

        import time
        start = time.perf_counter()
        try:
            from rag_system.rag_functions import build_vector_store
            build_vector_store(COLLECTION_NAME=str(user_id), PDF_PATH=pdf_path)
            from pdf_to_json.strategy_loader import convert_pdf_to_strategy
            strategy = convert_pdf_to_strategy(pdf_path=pdf_path)
            strategy_json = strategy.model_dump(mode="json")
            json.dumps(strategy_json)
            print(f"Took {time.perf_counter() - start:.2f} seconds")
        except Exception as exc:
            pdf_path.unlink(missing_ok=True)
            flash(str(exc), "error")
            return redirect(url_for("company"))

    logo_path = None
    if logo_bytes is not None:
        logo_folder = Path(app.root_path) / "static" / "company_logos" / user_id
        logo_folder.mkdir(parents=True, exist_ok=True)
        logo_filename = f"{uuid4().hex}.png"
        logo_file.stream.seek(0)
        logo_file.save(logo_folder / logo_filename)
        new_logo_path = logo_folder / logo_filename
        created_paths.append(new_logo_path)
        logo_path = f"company_logos/{user_id}/{logo_filename}"

    old_logo_path = company.logo_path if company else None
    if not company:
        company = CompanyInfo(user_id=user_id)
        db.session.add(company)
    company.brand_context = brand_context
    if pdf_path:
        company.content_strategy_path = str(pdf_path)
        company.content_strategy_json = strategy_json
    if logo_path:
        company.logo_path = logo_path

    try:
        db.session.commit()
    except Exception:
        db.session.rollback()
        for created_path in created_paths:
            created_path.unlink(missing_ok=True)
        flash("Unable to save company information. Please try again.", "error")
        return redirect(url_for("company"))

    if logo_path and old_logo_path and old_logo_path != logo_path:
        old_logo_file = Path(app.root_path) / "static" / old_logo_path
        try:
            old_logo_file.unlink(missing_ok=True)
        except OSError:
            app.logger.warning("Unable to remove replaced company logo %s", old_logo_file)

    flash("Company information saved successfully.", "success")
    return redirect(url_for("company"))


@app.route("/logout", methods=["POST", "GET"])
def logout():

    response = redirect(url_for("home"))
    unset_jwt_cookies(response)
    flash("You have been logged out successfully.", "success")
    return response

# this route recieves 'POST' request from javascript, streams generated content back to js(browser) that displays on html. 
@app.route(
        "/generate_content",
        methods=["POST"],
        strict_slashes=False,
    )
@jwt_required()
def generate_content():
    VALID_PLATFORMS = {"linkedin", "instagram", "facebook"}
    user_id = get_jwt_identity()
    payload = request.get_json(silent=True) or {}

    content_source = (payload.get("content_source") or "").strip()
    platform = (payload.get("platform") or "").strip().lower()
    user_input = (payload.get("user_input") or "").strip()

    valid_sources = {"inspiration", "existing_post", "generate"}

    if content_source not in valid_sources:
        return jsonify({
                "error": "Please choose a valid content source."
            }), 400

    if platform not in VALID_PLATFORMS:
        return jsonify({
                "error": "Please choose a valid platform."
            }), 400

    if content_source != "generate" and not user_input:
        return jsonify({
                "error": "Please provide input for the selected content source."
            }), 400

    brand_context = None
    company = CompanyInfo.query.filter_by(user_id=user_id).first()

    if company and company.brand_context:
        brand_context = company.brand_context

    from agents.user_topic_generator.functions import stream_generated_content

    def generate():
        try:
            for chunk in stream_generated_content(
                    platform=platform,
                    user_input=user_input,
                    content_source=content_source,
                    brand_context=brand_context,
                ):
                if chunk:
                    yield chunk
        except Exception as exc:
            print("CONTENT GENERATION ERROR:", exc, flush=True)
            traceback.print_exc()
            yield (
                    "\n\nUnable to generate content right now. "
                    "Please try again."
                )

    return Response(
            stream_with_context(generate()),
            mimetype="text/plain; charset=utf-8",
            headers={
                "Cache-Control": "no-cache, no-transform",
                "X-Accel-Buffering": "no",
                "Connection": "keep-alive",
            },
        )

# saves the scheduled data from create_content.html to db.
@app.route(
        "/schedule_content",
        methods=["POST"],
        strict_slashes=False,
    )
@jwt_required()
def schedule_content():
    VALID_PLATFORMS = {"linkedin", "instagram", "facebook"}
    user_id = get_jwt_identity()
    payload = request.get_json(silent=True) or {}

    platform = (payload.get("platform") or "").strip().lower()
    scheduled_at_raw = (payload.get("scheduled_at") or "").strip()
    post_content = payload.get("post_content")
    images = payload.get("images") or []
    hitl_required = payload.get("hitl_required") is True
    notify_hours_before = payload.get("notify_hours_before")
    unapproved_action = payload.get("unapproved_action")

    if hitl_required:
        try:
            notify_hours_before = int(notify_hours_before)
        except (TypeError, ValueError):
            notify_hours_before = None
        if notify_hours_before not in {1, 2, 4, 6, 8}:
            return jsonify({"error": "Choose a notification lead time."}), 400
        if unapproved_action not in {"publish", "skip"}:
            return jsonify({"error": "Choose what happens when the post is not approved."}), 400

    if isinstance(post_content, str):
        post_content = post_content.strip() or None
    else:
        post_content = None

    if platform not in VALID_PLATFORMS:
        return jsonify({
                "error": "Please choose a valid platform."
            }), 400

    if not isinstance(images, list) or len(images) > 10:
        return jsonify({"error": "A post can include up to 10 images."}), 400

    if platform == "instagram" and not images:
        return jsonify({"error": "Instagram posts need at least one JPG image."}), 400

    normalized_images = []
    for image in images:
        if not isinstance(image, dict):
            return jsonify({"error": "Invalid image details."}), 400
        filename = image.get("filename")
        image_url = image.get("image_url")
        safe_filename = secure_filename(filename or "")
        if not safe_filename or safe_filename != filename:
            return jsonify({"error": "An uploaded image is no longer available. Upload it again."}), 400
        extension = safe_filename.rsplit(".", 1)[-1].lower() if "." in safe_filename else ""
        if extension not in ALLOWED_IMAGE_EXTENSIONS:
            return jsonify({"error": "Unsupported image format."}), 400
        expected_path = url_for("static", filename=f"uploads/{safe_filename}")
        if not image_url or urlparse(image_url).path != expected_path:
            return jsonify({"error": "Invalid uploaded image reference."}), 400
        if not os.path.isfile(os.path.join(UPLOAD_FOLDER, safe_filename)):
            return jsonify({"error": "An uploaded image is no longer available. Upload it again."}), 400
        if platform == "instagram" and extension not in {"jpg", "jpeg"}:
            return jsonify({"error": "Instagram photos must be JPG or JPEG images."}), 400
        if platform == "linkedin" and extension not in {"jpg", "jpeg", "png"}:
            return jsonify({"error": "LinkedIn photos must be JPG or PNG images."}), 400
        normalized_images.append({
            "image_url": image_url,
            "filename": safe_filename,
            "source_path": os.path.join(UPLOAD_FOLDER, safe_filename),
        })

    account = Account.query.filter_by(user_id=user_id).first()
    connection_error = _connection_error(account, platform)
    if connection_error:
        return jsonify({"error": connection_error, "connect_required": True}), 400

    if not scheduled_at_raw:
        return jsonify({
                "error": "Please choose a date and time."
            }), 400

    try:
        parsed = datetime.fromisoformat(scheduled_at_raw)
    except ValueError:
        return jsonify({
                "error": "Please choose a valid date and time."
            }), 400

    company = CompanyInfo.query.filter_by(user_id=user_id).first()
    timezone_name = (
            company.timezone
            if company and company.timezone
            else "Asia/Kolkata"
        )

    try:
        timezone = ZoneInfo(timezone_name)
    except Exception:
        timezone = ZoneInfo("Asia/Kolkata")

    if parsed.tzinfo is None:
        scheduled_at = parsed.replace(tzinfo=timezone)
    else:
        scheduled_at = parsed.astimezone(timezone)

    scheduled_upload_folder = Path(UPLOAD_FOLDER).parent / "scheduled_uploads"
    scheduled_upload_folder.mkdir(parents=True, exist_ok=True)
    scheduled_images = []
    scheduled_image_paths = []
    try:
        for image in normalized_images:
            extension = image["filename"].rsplit(".", 1)[-1].lower()
            scheduled_filename = f"{uuid.uuid4().hex}.{extension}"
            scheduled_path = scheduled_upload_folder / scheduled_filename
            shutil.copy2(image["source_path"], scheduled_path)
            scheduled_image_paths.append(scheduled_path)
            scheduled_images.append({
                "image_url": url_for(
                    "static",
                    filename=f"scheduled_uploads/{scheduled_filename}",
                    _external=True
                ),
                "filename": scheduled_filename,
            })
    except OSError as exc:
        for scheduled_path in scheduled_image_paths:
            scheduled_path.unlink(missing_ok=True)
        return jsonify({"error": "Unable to prepare the photos for scheduling."}), 500

    job = ContentJob(
            user_id=user_id,
            platform=platform,
            post_content=post_content,
            images=scheduled_images,
            generation_source=(payload.get("generation_source") or "").strip() or None,
            generation_input=payload.get("generation_input") if isinstance(payload.get("generation_input"), str) else None,
            scheduled_at=scheduled_at,
            status="scheduled",
            hitl_required=hitl_required,
            notify_hours_before=notify_hours_before if hitl_required else None,
            publish_if_unapproved=(unapproved_action == "publish") if hitl_required else True,
            approval_status="pending" if hitl_required else "not_required",
            updated_at=datetime.now(UTC),
        )

    try:
        db.session.add(job)
        db.session.commit()
    except Exception as exc:
        db.session.rollback()
        for scheduled_path in scheduled_image_paths:
            scheduled_path.unlink(missing_ok=True)
        print("SCHEDULE SAVE ERROR:", exc, flush=True)
        traceback.print_exc()
        return jsonify({
                "error": "Unable to save the schedule. Please try again."
            }), 500

    # for image in normalized_images:
    #     try:
    #         os.remove(image["source_path"])
    #     except OSError as cleanup_error:
    #         print("TEMP IMAGE DELETE ERROR:", repr(cleanup_error), flush=True)

    flash("Content scheduled successfully.", "success")
    flashes = [
            {"category": category, "message": message}
            for category, message in get_flashed_messages(with_categories=True)
        ]

    return jsonify({
            "ok": True,
            "message": "Content scheduled successfully.",
            "flashes": flashes,
        })

# Sends the scheduled jobs data to the browser, that to be displayed in the calendar (content_calendar.html).
@app.route("/api/content-calendar", methods=["GET"])
@jwt_required()
def get_content_calendar():

    user_id = get_jwt_identity()

    recurring_posts = RecurringContent.query.filter(
        RecurringContent.user_id == user_id,
        RecurringContent.status == "scheduled"
    ).all()

    custom_posts = ContentJob.query.filter(
        ContentJob.user_id == user_id,
        ContentJob.status == "scheduled"
    ).all()

    events = []

    # Recurring posts
    for post in recurring_posts:

        image_urls = [
            url_for("static", filename=f"scheduled_uploads/{secure_filename(image['filename'])}")
            for image in (post.images or [])
            if isinstance(image, dict) and image.get("filename")
            and secure_filename(image["filename"]) == image["filename"]
        ]
        if not image_urls and post.image_filename:
            image_urls = [url_for("static", filename=f"scheduled_uploads/{secure_filename(post.image_filename)}")]

        events.append({
            "id": f"recurring-{post.id}",
            "title": post.post_content[:50],
            "start": post.scheduled_at.isoformat(),
            "extendedProps": {
                "status": post.status,
                "platform": post.platform,
                "post_content": post.post_content,
                "image_url": image_urls[0] if image_urls else None,
                "image_urls": image_urls,
            }
        })

    # Custom posts
    for post in custom_posts:

        image_urls = [
            url_for("static", filename=f"scheduled_uploads/{secure_filename(image['filename'])}")
            for image in (post.images or [])
            if isinstance(image, dict) and image.get("filename")
            and secure_filename(image["filename"]) == image["filename"]
        ]

        events.append({
            "id": f"custom-{post.id}",
            "title": post.post_content[:50],
            "start": post.scheduled_at.isoformat(),
            "extendedProps": {
                "status": post.status,
                "platform": post.platform,
                "post_content": post.post_content,
                "image_url": image_urls[0] if image_urls else None,
                "image_urls": image_urls,
            }
        })

    # Earliest scheduled content first
    events.sort(
        key=lambda event: event["start"]
    )

    return jsonify(events)


def _get_pending_post(kind, post_id, user_id):
    if kind == "content_job":
        model = ContentJob
    elif kind == "recurring":
        model = RecurringContent
    else:
        return None
    return model.query.filter_by(id=post_id, user_id=user_id, status="scheduled").first()


def _save_pending_images(post, kind, images):
    if not isinstance(images, list) or len(images) > 10:
        raise ValueError("A post can include up to 10 images.")
    if post.platform == "instagram" and not images:
        raise ValueError("Instagram posts need at least one JPG image.")

    folder = Path(UPLOAD_FOLDER).parent / "scheduled_uploads"
    folder.mkdir(parents=True, exist_ok=True)
    saved_images = []
    created_files = []
    for image in images:
        if not isinstance(image, dict):
            raise ValueError("Invalid image details.")
        filename = image.get("filename")
        safe_filename = secure_filename(filename or "")
        parsed_path = urlparse(image.get("image_url") or "").path
        if not safe_filename or safe_filename != filename:
            raise ValueError("An image reference is invalid. Upload it again.")
        extension = safe_filename.rsplit(".", 1)[-1].lower() if "." in safe_filename else ""
        if extension not in ALLOWED_IMAGE_EXTENSIONS:
            raise ValueError("Unsupported image format.")
        if post.platform == "instagram" and extension not in {"jpg", "jpeg"}:
            raise ValueError("Instagram carousel photos must be JPG or JPEG.")
        if post.platform == "linkedin" and extension not in {"jpg", "jpeg", "png"}:
            raise ValueError("LinkedIn photos must be JPG or PNG.")

        if parsed_path == f"/static/uploads/{safe_filename}":
            source_path = Path(UPLOAD_FOLDER) / safe_filename
        elif parsed_path == f"/static/scheduled_uploads/{safe_filename}":
            source_path = folder / safe_filename
        else:
            raise ValueError("Invalid image reference. Upload the image again.")
        if not source_path.is_file():
            raise ValueError("An image is no longer available. Upload it again.")

        target_name = f"{uuid.uuid4().hex}.{extension}"
        target_path = folder / target_name
        shutil.copy2(source_path, target_path)
        created_files.append(target_path)
        saved_images.append({
            "image_url": url_for("static", filename=f"scheduled_uploads/{target_name}", _external=True),
            "filename": target_name,
        })
    return saved_images, created_files


def _existing_pending_image_names(post, kind):
    images = post.images or []
    names = [image.get("filename") for image in images if isinstance(image, dict)]
    if kind == "recurring" and not names and post.image_filename:
        names.append(post.image_filename)
    return {name for name in names if name}


@app.route("/api/pending-posts/<kind>/<int:post_id>", methods=["PUT", "DELETE"])
@jwt_required()
def edit_pending_post(kind, post_id):
    user_id = get_jwt_identity()
    post = _get_pending_post(kind, post_id, user_id)
    if not post:
        return jsonify({"error": "This scheduled post is no longer available."}), 404

    if request.method == "DELETE":
        image_names = _existing_pending_image_names(post, kind)
        image_paths = [Path(UPLOAD_FOLDER).parent / "scheduled_uploads" / name for name in image_names]
        if kind == "recurring" and post.image_path:
            image_paths.append(Path(post.image_path))
        db.session.delete(post)
        db.session.commit()
        for image_path in image_paths:
            try:
                image_path.unlink(missing_ok=True)
            except OSError:
                app.logger.warning("Could not delete scheduled image %s", image_path)
        return jsonify({"ok": True})

    payload = request.get_json(silent=True) or {}
    content = payload.get("post_content")
    if not isinstance(content, str) or not content.strip():
        return jsonify({"error": "Post content is required."}), 400
    old_names = _existing_pending_image_names(post, kind)
    created_files = []
    try:
        images, created_files = _save_pending_images(post, kind, payload.get("images") or [])
        post.post_content = content.strip()
        post.images = images
        if kind == "recurring":
            first = images[0] if images else None
            post.image_filename = first["filename"] if first else None
            post.image_path = str((Path(UPLOAD_FOLDER).parent / "scheduled_uploads" / first["filename"]).resolve()) if first else None
            post.image_status = "uploaded" if images else None
        db.session.commit()
    except ValueError as exc:
        for image_path in created_files:
            image_path.unlink(missing_ok=True)
        return jsonify({"error": str(exc)}), 400
    except Exception:
        db.session.rollback()
        for image_path in created_files:
            image_path.unlink(missing_ok=True)
        app.logger.exception("Could not update pending post")
        return jsonify({"error": "Unable to save changes. Please try again."}), 500

    new_names = {image["filename"] for image in images}
    for old_name in old_names - new_names:
        (Path(UPLOAD_FOLDER).parent / "scheduled_uploads" / old_name).unlink(missing_ok=True)
    return jsonify({"ok": True, "post_content": post.post_content, "images": images})


@app.route("/api/pending-posts/<kind>/<int:post_id>/<decision>", methods=["POST"])
@jwt_required()
def decide_pending_post(kind, post_id, decision):
    if decision not in {"approve", "reject"}:
        return jsonify({"error": "Choose approve or reject."}), 400
    post = _get_pending_post(kind, post_id, get_jwt_identity())
    if not post:
        return jsonify({"error": "This scheduled post is no longer available."}), 404

    if decision == "approve":
        post.approval_status = "approved"
        post.approved_at = datetime.now(UTC)
        db.session.commit()
        return jsonify({"ok": True, "approval_status": "approved"})

    post.approval_status = "rejected"
    post.status = "rejected"
    db.session.commit()
    return jsonify({"ok": True, "approval_status": "rejected"})


@app.route("/api/pending-posts/<kind>/<int:post_id>/regenerate", methods=["POST"])
@jwt_required()
def regenerate_pending_post(kind, post_id):
    user_id = get_jwt_identity()
    post = _get_pending_post(kind, post_id, user_id)
    if not post:
        return jsonify({"error": "This scheduled post is no longer available."}), 404
    today = datetime.now(UTC).date()
    if post.regeneration_date != today:
        post.regeneration_date = today
        post.regeneration_count = 0
    if post.regeneration_count >= 3:
        db.session.commit()
        return jsonify({"error": "This post has reached its 3 regenerations for today.", "remaining": 0}), 429

    if kind == "content_job":
        has_saved_inputs = bool(post.generation_source)
    else:
        context = post.generation_context
        has_saved_inputs = isinstance(context, dict) and bool(context.get("topic"))
    post.regeneration_count = (post.regeneration_count or 0) + 1
    db.session.commit()
    remaining = 3 - post.regeneration_count
    try:
        if kind == "content_job":
            company = CompanyInfo.query.filter_by(user_id=user_id).first()
            from agents.user_topic_generator.functions import stream_generated_content
            generated = "".join(chunk for chunk in stream_generated_content(
                platform=post.platform,
                user_input=(post.generation_input or "") if has_saved_inputs else (post.post_content or ""),
                content_source=post.generation_source if has_saved_inputs else "existing_post",
                brand_context=company.brand_context if company else None
            ) if chunk)
        elif has_saved_inputs:
            from agents.content_writer_agent.content_functions import (
                generate_linkedin_content, generate_facebook_content, generate_instagram_content
            )
            guidelines = context.get("pillar_guidelines")
            if guidelines is None:
                from rag_system.rag_functions import retrieve_semantic_chunks
                guidelines = retrieve_semantic_chunks(pillar=context["pillar"], user_id=user_id)
            writers = {
                "linkedin": generate_linkedin_content,
                "facebook": generate_facebook_content,
                "instagram": generate_instagram_content,
            }
            result = writers[post.platform](
                pillar=context["pillar"], topic=context["topic"],
                post_format=context["post_format"], brand_voice=context["brand_voice"],
                pillar_guidlines=guidelines
            )
            generated = result.content if hasattr(result, "content") else str(result)
        else:
            company = CompanyInfo.query.filter_by(user_id=user_id).first()
            from agents.user_topic_generator.functions import stream_generated_content
            generated = "".join(chunk for chunk in stream_generated_content(
                platform=post.platform,
                user_input=post.post_content or "",
                content_source="existing_post",
                brand_context=company.brand_context if company else None
            ) if chunk)
        generated = generated.strip()
        if not generated or "Unable to generate content right now" in generated:
            return jsonify({"error": "Unable to regenerate content right now. Please try again.", "remaining": remaining}), 502
        return jsonify({"ok": True, "post_content": generated, "remaining": remaining})
    except Exception:
        db.session.rollback()
        app.logger.exception("Pending post regeneration failed")
        return jsonify({"error": "Unable to regenerate content right now. Please try again.", "remaining": remaining}), 502


def _connection_error(account, platform):
    """Return a user-facing publishing error when a platform is not ready."""
    requirements = {
        "linkedin": ("linkedin_access_token", "author_urn", "linkedin_token_expires_at"),
        "facebook": ("page_id", "page_access_token", "meta_token_expires_at"),
        "instagram": ("instagram_business_id", "page_access_token", "meta_token_expires_at"),
    }
    platform_name = platform.title()
    if not account:
        return f"Connect your {platform_name} account in Social Accounts before publishing."

    fields = requirements.get(platform)
    if not fields or any(not getattr(account, field, None) for field in fields[:2]):
        return f"Connect your {platform_name} account in Social Accounts before publishing."

    expires_at = getattr(account, fields[2], None)
    if expires_at:
        if expires_at.tzinfo is None:
            expires_at = expires_at.replace(tzinfo=UTC)
        if expires_at <= datetime.now(UTC):
            return f"Your {platform_name} connection has expired. Reconnect it in Social Accounts before publishing."

    return None

# publishes the post for 'Publish Now' button in create_content.html
@app.route("/api/publish-content", methods=["POST"])
@jwt_required()
def publish_content():

    from publisher.publisher_functions import (
        publish_to_facebook,
        publish_to_instagram,
        publish_to_linkedin
    )

    user_id = get_jwt_identity()

    data = request.get_json(silent=True) or {}

    content = data.get("content")
    content = content.strip() if isinstance(content, str) else ""
    platform = (data.get("platform") or "").strip().lower()
    images = data.get("images")

    # Keep compatibility with older clients that send one image.
    if images is None and data.get("image_url"):
        images = [{
            "image_url": data.get("image_url"),
            "filename": data.get("image_filename"),
        }]
    images = images or []


    if not content:
        return jsonify({
            "success": False,
            "error": "Content is required."
        }), 400

    if platform not in {"linkedin", "facebook", "instagram"}:
        return jsonify({
            "success": False,
            "error": "Choose a valid platform before publishing."
        }), 400

    if not isinstance(images, list) or len(images) > 10:
        return jsonify({
            "success": False,
            "error": "A post can include up to 10 images."
        }), 400

    if platform == "instagram" and not images:
        return jsonify({
            "success": False,
            "error": "Instagram publishing requires at least one JPG image."
        }), 400

    image_urls = []
    image_paths = []
    for image in images:
        if not isinstance(image, dict):
            return jsonify({"success": False, "error": "Invalid image details."}), 400

        filename = image.get("filename")
        image_url = image.get("image_url")
        safe_filename = secure_filename(filename or "")
        if not safe_filename or safe_filename != filename:
            return jsonify({"success": False, "error": "An uploaded image is no longer available. Upload it again."}), 400

        extension = safe_filename.rsplit(".", 1)[-1].lower() if "." in safe_filename else ""
        if extension not in ALLOWED_IMAGE_EXTENSIONS:
            return jsonify({"success": False, "error": "Unsupported image format."}), 400

        expected_path = url_for("static", filename=f"uploads/{safe_filename}")
        if not image_url or urlparse(image_url).path != expected_path:
            return jsonify({"success": False, "error": "Invalid uploaded image reference."}), 400

        image_path = os.path.join(UPLOAD_FOLDER, safe_filename)
        if not os.path.isfile(image_path):
            return jsonify({"success": False, "error": "An uploaded image is no longer available. Upload it again."}), 400

        if platform == "instagram" and extension not in {"jpg", "jpeg"}:
            return jsonify({"success": False, "error": "Instagram photos must be JPG or JPEG images."}), 400
        if platform == "linkedin" and extension not in {"jpg", "jpeg", "png"}:
            return jsonify({"success": False, "error": "LinkedIn photos must be JPG or PNG images."}), 400

        image_urls.append(image_url)
        image_paths.append(image_path)

    account = Account.query.filter_by(user_id=user_id).first()
    connection_error = _connection_error(account, platform)
    if connection_error:
        return jsonify({
            "success": False,
            "connect_required": True,
            "error": connection_error
        }), 400

    try:

        if platform == "linkedin":

            publish_to_linkedin(
                message=content,
                user_id=user_id,
                image_paths=image_paths
            )

        elif platform == "facebook":

            publish_to_facebook(
                message=content,
                user_id=user_id,
                image_urls=image_urls
            )

        elif platform == "instagram":

            publish_to_instagram(
                message=content,
                user_id=user_id,
                image_urls=image_urls
            )

        else:

            return jsonify({
                "success": False,
                "error": f"Unsupported platform: {platform}"
            }), 400

        # Deleting the file after upload.

        # for image_path in image_paths:
        #     try:
        #         os.remove(image_path)
        #     except OSError as cleanup_error:
        #         print("IMAGE DELETE ERROR:", repr(cleanup_error), flush=True)


        return jsonify({
            "success": True,
            "message":
                f"Content published successfully to {platform.title()}."
        }), 200

    except Exception as e:

        print(
            "Publish content error:",
            repr(e)
        )

        return jsonify({
            "success": False,
            "error": str(e)
        }), 500

# ==================================================
# IMAGE GENERATOR PROMPT THROUGH LLM


@app.route("/generate_image_prompt", methods=["POST"])
def generate_image_prompt_route():
    """
    Generate an image-generation prompt from the final
    social media post content.
    """
    print("IMAGE PROMPT ROUTE HIT")

    
    from model import llm

    print("LLM")


    try:

        data = request.get_json(silent=True) or {}

        content = data.get("content")
        platform = data.get("platform")

        if not content or not content.strip():
            return jsonify({
                "success": False,
                "error": "Content is required."
            }), 400

        if not platform or not platform.strip():
            return jsonify({
                "success": False,
                "error": "Platform is required."
            }), 400

        print("CALLING IMAGE PROMPT GENERATOR")

        

        image_prompt = generate_image_prompt(
            content=content,
            platform=platform
        )

        print("IMAGE PROMPT GENERATED")
        print("IMAGE PROMPT LENGTH:", len(image_prompt))


        return jsonify({
            "success": True,
            "image_prompt": image_prompt
        }), 200

    except Exception as e:

        print(
            "Image prompt generation error:",
            repr(e)
        )

        return jsonify({
            "success": False,
            "error": str(e)
        }), 500

@app.route("/upload_image", methods=["POST"])
@jwt_required()
def upload_image():
    """
    Upload one or more images to the server and return public URLs.
    """

    try:
        images = request.files.getlist("images") or request.files.getlist("image")
        images = [image for image in images if image and image.filename]

        if not images:
            return jsonify({
                "success": False,
                "error": "Choose at least one image to upload."
            }), 400

        if len(images) > 10:
            return jsonify({
                "success": False,
                "error": "You can upload up to 10 images per post."
            }), 400

        validated_images = []
        for image in images:
            original_filename = secure_filename(image.filename)
            extension = (
                original_filename.rsplit(".", 1)[1].lower()
                if "." in original_filename
                else ""
            )

            if extension not in ALLOWED_IMAGE_EXTENSIONS:
                return jsonify({
                    "success": False,
                    "error": "Only PNG, JPG, JPEG and WEBP images are allowed."
                }), 400

            validated_images.append((image, original_filename, extension))

        uploaded_images = []
        for image, original_filename, extension in validated_images:
            unique_filename = f"{uuid.uuid4().hex}.{extension}"
            file_path = os.path.join(UPLOAD_FOLDER, unique_filename)
            image.save(file_path)

            image_url = url_for(
                "static",
                filename=f"uploads/{unique_filename}",
                _external=True
            )
            uploaded_images.append({
                "image_url": image_url,
                "filename": unique_filename,
                "name": original_filename
            })

        return jsonify({
            "success": True,
            "images": uploaded_images
        }), 200

    except Exception as e:
        print("IMAGE UPLOAD ERROR:", repr(e))

        return jsonify({
            "success": False,
            "error": str(e)
        }), 500


@app.route("/upload_image/<filename>", methods=["DELETE"])
@jwt_required()
def delete_uploaded_image(filename):
    safe_filename = secure_filename(filename or "")
    if not safe_filename or safe_filename != filename:
        return jsonify({"success": False, "error": "Invalid image reference."}), 400

    image_path = os.path.join(UPLOAD_FOLDER, safe_filename)
    # if os.path.isfile(image_path):
    #     os.remove(image_path)
    return jsonify({"success": True}), 200

@app.route("/uploads/<filename>")
def uploaded_image(filename):

    from flask import send_from_directory

    return send_from_directory(
        UPLOAD_FOLDER,
        filename
    )

if __name__ == "__main__":

    from scheduler import start_scheduler

    start_scheduler()
    app.run(
        host="0.0.0.0",
        port=int(os.environ.get("PORT", 5000)),
        debug=False,
        threaded=True
    )
