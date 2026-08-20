import os
import psutil
from zoneinfo import ZoneInfo
process = psutil.Process(os.getpid())

def mem(label):
    rss = process.memory_info().rss / 1024 / 1024
    print(f"[MEMORY] {label}: {rss:.1f} MB", flush=True)


mem("startup")

import json
import traceback
mem("json + traceback")

from app import app, db, jwt
mem("app")

from uuid import uuid4
from pathlib import Path
mem("uuid + pathlib")

from ruamel.yaml import YAML
mem("ruamel")

from dotenv import load_dotenv
mem("dotenv")

from flask_sqlalchemy import SQLAlchemy
mem("flask-sqlalchemy")

from initialize_database.models import Account, PublishedPost, User, CompanyInfo , ContentJob , RecurringContent
mem("models")

from datetime import UTC, datetime, timedelta
mem("datetime")

from cron_converter.cron_conversion import convert_to_cron
mem("cron-converter")

from werkzeug.security import check_password_hash, generate_password_hash
mem("werkzeug")

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
mem("flask")

from flask_jwt_extended import (
    create_access_token,
    get_jwt_identity,
    jwt_required,
    verify_jwt_in_request,
    unset_jwt_cookies
)
mem("jwt")

from services.linkedin_services import (
    get_author_urn,
    logging_in,
    retrive_auth_code,
    token_exchange,
)
mem("linkedin")

from services.meta_services import (
    exchange_meta_token,
    get_instagram_business,
    get_long_lived_token,
    get_pages,
    get_user_info,
    meta_login,
    retrieve_meta_auth_code,
)
mem("meta")

load_dotenv()

UPLOAD_FOLDER = Path("uploads")
UPLOAD_FOLDER.mkdir(exist_ok=True)

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


def render_page(template_name, title, active_page):
    return render_template(template_name, title=title, active_page=active_page)

# Paging routing for the Flask application

@app.route("/")
def home():
    return render_page("home.html", "Home — ELVA SocialAI", "home")


@app.route("/about")
def about():
    return render_page("about.html", "About Us — ELVA SocialAI", "about")


@app.route("/oauth")
def oauth():
    return render_page("oauth.html", "OAuth Login — ELVA SocialAI", "oauth")


@app.route("/company")
def company():
    return render_page("company.html", "Company Data — ELVA SocialAI", "company")


@app.route("/schedule")
def schedule():
    return render_page("schedule.html", "Schedule Content — ELVA SocialAI", "schedule")

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

    return render_template(
        "posts.html", title="Published Posts — ELVA SocialAI", active_page="posts", posts=posts
    )

@app.route("/login", methods=["GET", "POST"])
def login():
    print("🔥 LOGIN ROUTE HIT:", request.method, flush=True)

    if request.method == "POST":

        email = request.form.get("email", "").strip().lower()

        print("🔥 LOGIN POST RECEIVED", flush=True)

        print("🔥 EMAIL RECEIVED:", email, flush=True)

        password = request.form.get("password", "")
        print("🔥 PASSWORD RECEIVED:", bool(password), flush=True)

        print("🔥 ABOUT TO QUERY DATABASE", flush=True)


        user = User.query.filter_by(
            email=email
        ).first()

        if not user:
            print("GOT THE USER DATA", flush=True)

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
            identity=user.user_id
        )

        response = make_response(
            redirect(url_for("home"))
        )

        print("SENDING COOKIE" , flush=True)

        response.set_cookie(
            "access_token",
            access_token,
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

@app.route("/schedule_post", methods=["POST"])
@jwt_required()
def schedule_post():

    user_id = get_jwt_identity()

    input_text = request.form.get("schedule_task")
    timezone = request.form.get("timezone")

    platforms = request.form.getlist("platforms")

    if not input_text:
        return "Schedule instruction is required.", 400

    if not platforms:
        return "Please select at least one platform.", 400

    cron_expression = convert_to_cron(input_text)

    print("CRON:", cron_expression)
    print("TIME ZONE:", timezone)
    print("PLATFORMS:", platforms)

    company = CompanyInfo.query.filter_by(
        user_id=user_id
    ).first()

    if not company:

        company = CompanyInfo(
            user_id=user_id,
            scheduled_time=cron_expression,
            timezone=timezone,
            platforms=platforms
        )

        db.session.add(company)

    else:

        company.scheduled_time = cron_expression
        company.timezone = timezone
        company.platforms = platforms

    db.session.commit()

    flash(
        "Schedule updated successfully",
        "success"
    )

    return redirect(url_for("schedule"))
# Connecting to LinkedIN

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

# Connect to Meta
@app.route("/connect_meta")
@jwt_required()
def connect_meta():

    meta_url = meta_login()

    return redirect(meta_url)

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
        long_token, expires_in = get_long_lived_token(short_token)
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

    # For now, connect the first Page.
    # Later, allow the user to choose one.
    page = pages[0]

    page_id = page["id"]
    page_name = page["name"]
    page_token = page["access_token"]

    # Step 6: Retrieve the linked Instagram Business Account
    try:
        instagram = get_instagram_business(page_id, page_token)
    except RuntimeError as e:
        return str(e), 500

    instagram_id = (
        instagram["id"]
        if instagram
        else None
    )

    account = Account.query.filter_by(user_id=user_id).first()

    if account:
        account.page_name = page_name
        account.page_id = page_id
        account.page_access_token = page_token
        account.instagram_business_id = instagram_id
    else:
        account = Account(
            user_id=user_id,
            page_name=page_name,
            page_id=page_id,
            page_access_token=page_token,
            instagram_business_id=instagram_id
        )
        db.session.add(account)

    db.session.commit()
    flash("Meta Account connected Successfully" , "Success")
    
    return redirect(url_for('oauth'))


@app.route('/save_company_data' , methods = ['POST'])
@jwt_required()
def save_company_info():
    user_id = get_jwt_identity()

    brand_context = request.form.get(
        "brand_context",
        ""
    ).strip()

    pdf = request.files.get("strategy_pdf")

    if not brand_context:
        flash("Please enter brand context." , "error")
        return redirect(url_for("company"))

    if not pdf:
        flash("Please upload a PDF.", "error")
        return redirect(url_for("company"))

    if pdf.filename == "":
        flash("Please select a PDF.", "error")
        return redirect(url_for("company"))

    if not pdf.filename.lower().endswith(".pdf"):
        flash("Only PDF files are allowed." , "error")
        return redirect(url_for("company"))

    #save pdf

    user_folder = UPLOAD_FOLDER / user_id
    user_folder.mkdir(parents=True, exist_ok=True)

    filename = f"{uuid4().hex}.pdf"
    pdf_path = user_folder / filename
    pdf.save(pdf_path)

    # create embeddings

    collection_name = str(user_id)

    import time 
    start = time.perf_counter()

    try:
        from rag_system.rag_functions import build_vector_store        
        build_vector_store(COLLECTION_NAME=collection_name , PDF_PATH=pdf_path)

    except Exception as e:
        print("ERROR:", e)
        traceback.print_exc()
        flash(str(e) , "error")
        return redirect(url_for("company"))


    try:

        from pdf_to_json.strategy_loader import convert_pdf_to_strategy
        strategy = convert_pdf_to_strategy(pdf_path=pdf_path)
    except Exception as e:
        flash(f"{e}", "error")
        return redirect(url_for('company'))

    end = time.perf_counter()
    print(f"Took {end - start:.2f} seconds")

    strategy_json = strategy.model_dump(mode="json")

    # Test serialization
    json.dumps(strategy_json)
    
    company = CompanyInfo.query.filter_by(
        user_id=user_id
    ).first()

    if company:
        company.brand_context = brand_context
        company.content_strategy_path = str(pdf_path)
        company.content_strategy_json = strategy_json

    else:
        company = CompanyInfo(
            user_id=user_id,
            brand_context=brand_context,
            content_strategy_path=str(pdf_path),
            content_strategy_json= strategy_json
        )

        db.session.add(company)

    db.session.commit()
    flash("Company information saved successfully." , "success")

    return redirect(url_for("company"))


@app.route("/logout", methods=["POST", "GET"])
def logout():

    response = redirect(url_for("home"))
    unset_jwt_cookies(response)
    flash("You have been logged out successfully.", "success")
    return response

# generrate content routes
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

    if isinstance(post_content, str):
        post_content = post_content.strip() or None
    else:
        post_content = None

    if platform not in VALID_PLATFORMS:
        return jsonify({
                "error": "Please choose a valid platform."
            }), 400

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

    job = ContentJob(
            user_id=user_id,
            platform=platform,
            post_content=post_content,
            scheduled_at=scheduled_at,
            status="scheduled",
            updated_at=datetime.now(UTC),
        )

    try:
        db.session.add(job)
        db.session.commit()
    except Exception as exc:
        db.session.rollback()
        print("SCHEDULE SAVE ERROR:", exc, flush=True)
        traceback.print_exc()
        return jsonify({
                "error": "Unable to save the schedule. Please try again."
            }), 500

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
        events.append({
                "id": f"recurring-{post.id}",
                "title": post.post_content[:50],
                "start": post.scheduled_at.isoformat(),
                "extendedProps": {
                    "status": post.status,
                    "platform": post.platform,
                    "post_content": post.post_content
                }
            })

        # Custom posts
    for post in custom_posts:
            events.append({
                "id": f"custom-{post.id}",
                "title": post.post_content[:50],
                "start": post.scheduled_at.isoformat(),
                "extendedProps": {
                    "status": post.status,
                    "platform": post.platform,
                    "post_content": post.post_content
                }
            })

        # Earliest scheduled content first
    events.sort(
            key=lambda event: event["start"]
        )

    return jsonify(events)

@app.route("/api/publish-content", methods=["POST"])
@jwt_required()
def publish_content():

    from publisher.facebook_functions import publish_to_facebook , publish_to_instagram , publish_to_linkedin

    user_id = get_jwt_identity()

    data = request.get_json()

    content = data.get("content")
    platform = data.get("platform")

    if not content:
        return jsonify({
            "error": "Content is required."
        }), 400

    if not platform:
        return jsonify({
            "error": "Platform is required."
        }), 400

    try:

        if platform == "linkedin":

            publish_to_linkedin(
                message=content,
                user_id=user_id
            )

        elif platform == "facebook":

            publish_to_facebook(
                message=content,
                user_id=user_id
            )

        elif platform == "instagram":

            return jsonify({
                "error":
                    "Instagram publishing requires an image."
            }), 400

        else:

            return jsonify({
                "error":
                    f"Unsupported platform: {platform}"
            }), 400

        return jsonify({
            "success": True,
            "message":
                f"Content published successfully to {platform.title()}."
        })

    except Exception as e:

        print(
            "Publish content error:",
            repr(e)
        )

        return jsonify({
            "success": False,
            "error": str(e)
        }), 500

if __name__ == "__main__":

    from scheduler import start_scheduler

    start_scheduler()
    app.run(
        host="0.0.0.0",
        port=int(os.environ.get("PORT", 5000)),
        debug=False,
        threaded=True
    )