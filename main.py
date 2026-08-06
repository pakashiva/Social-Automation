import os
from app import app, db
from pathlib import Path
from ruamel.yaml import YAML
from dotenv import load_dotenv
from flask_sqlalchemy import SQLAlchemy
from app_configuration.app_config import Config
from initialize_database.models import Account, PublishedPost, User
from datetime import UTC, datetime, timedelta
from cron_converter.cron_conversion import convert_to_cron
from werkzeug.security import check_password_hash, generate_password_hash
from flask import Flask, flash, make_response, redirect, render_template, request, url_for
from flask_jwt_extended import (
    JWTManager,
    create_access_token,
    get_jwt_identity,
    jwt_required,
)
from services.linkedin_services import (
    add_to_database,
    get_author_urn,
    logging_in,
    retrive_auth_code,
    token_exchange,
)
from services.meta_services import (
    add_meta_to_database,
    exchange_meta_token,
    get_instagram_business,
    get_long_lived_token,
    get_pages,
    get_user_info,
    meta_login,
    retrieve_meta_auth_code,
)


load_dotenv()

app.config["JWT_SECRET_KEY"] = os.getenv("JWT_SECRET_KEY")
app.config["JWT_TOKEN_LOCATION"] = ["cookies"]
app.config["JWT_ACCESS_COOKIE_NAME"] = "access_token"
app.config["JWT_COOKIE_CSRF_PROTECT"] = False   # Enable in production

jwt = JWTManager(app)

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


def render_page(template_name, title, active_page):
    return render_template(template_name, title=title, active_page=active_page)

# Paging routing for the Flask application

@app.route("/")
def home():
    return render_page("home.html", "Home — CorpAI Media", "home")


@app.route("/about")
def about():
    return render_page("about.html", "About Us — CorpAI Media", "about")


@app.route("/oauth")
def oauth():
    return render_page("oauth.html", "OAuth Login — CorpAI Media", "oauth")


@app.route("/company")
def company():
    return render_page("company.html", "Company Data — CorpAI Media", "company")


@app.route("/schedule")
def schedule():
    return render_page("schedule.html", "Schedule Content — CorpAI Media", "schedule")

@app.route("/posts")
def posts():
    posts = PublishedPost.query.order_by(
    PublishedPost.published_at.desc()
).all()

    return render_template(
        "posts.html", title="Published Posts — CorpAI Media", active_page="posts", posts=posts
    )

@app.route("/login", methods=["GET", "POST"])
def login():

    if request.method == "POST":

        email = request.form.get(
            "email",
            ""
        ).strip().lower()

        password = request.form.get(
            "password",
            ""
        )

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

        access_token = create_access_token(
            identity=user.user_id
        )

        response = make_response(
            redirect(url_for("dashboard"))
        )

        response.set_cookie(
            "access_token",
            access_token,
            httponly=True,
            secure=False,      # True in production (HTTPS)
            samesite="Lax"
        )
        flash("Logged in successfully." , "success")

        return response

    return render_page(
        "login.html",
        "Login — CorpAI Media",
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
        "Sign Up — CorpAI Media",
        "signup"
    )

@app.route("/schedule_post", methods=["POST"])
@jwt_required()
def schedule_post():
    print("Route initialized")

    input_text = request.form.get("schedule_task")

    if not input_text:
        return "Schedule instruction is required.", 400

    cron_expression = convert_to_cron(input_text)
    print(cron_expression)

    workflow_path = ".github/workflows/schedular.yml"

    yaml = YAML()
    yaml.preserve_quotes = True

    # Read workflow
    with open(workflow_path, "r") as f:
        workflow = yaml.load(f)

    # Update schedule
    workflow["on"]["schedule"] = [
        {"cron": cron_expression}
    ]

    # Save workflow
    with open(workflow_path, "w") as f:
        yaml.dump(workflow, f)

    print(workflow.keys())

    return "Schedule updated successfully."


# Connecting to LinkedIN

@app.route('/connect_linkedIn')
@jwt_required()
def connect():
    linkedin_url = logging_in()
    return redirect(linkedin_url)  # callback route will be called after successful login


# route to retrieve Auth code, token exchange , Saving in Database
@app.route('/callback')
def callback():
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

    account = Account(
    company_name="ABC Company",
    access_token=access_token,
    author_urn=author_urn,
    expires_at=expires_at   
)
    add_to_database(account)

    return "LinkedIn account connected successfully."

# Connect to Meta
@app.route("/connect_meta")
@jwt_required()
def connect_meta():

    meta_url = meta_login()

    return redirect(meta_url)

@app.route("/meta_callback")
def meta_callback():

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


    # Step 7: Save account
    account = Account(
        page_name=page_name,
        page_id=page_id,
        page_access_token=page_token,
        instagram_business_id=instagram_id,
    )

    add_meta_to_database(account)

    return "Meta account connected successfully."

if __name__ == "__main__":
    app.run(port=5000, debug=True)
