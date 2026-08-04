from flask import Flask, render_template , redirect
from services.linkedin_services import (logging_in , retrive_auth_code , 
                                        token_exchange, add_to_database)
from flask_sqlalchemy import SQLAlchemy
from dotenv import load_dotenv
import os
from pathlib import Path
from app_configuration.linkedin_app import LinkedIn_app_config
from init_db.linkedin_accounts.models import LinkedInAccount
from app import db, app 

load_dotenv()

def render_page(template_name, title, active_page):
    return render_template(template_name, title=title, active_page=active_page)

# Paging routes for the Flask application

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
    return render_page("posts.html", "Published Posts — CorpAI Media", "posts")


# Connecting to the LinkedIN
@app.route('/connect_linkedIn')
def login():
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
        encrypted_token, expires_at = token_exchange(auth_code)
    except RuntimeError as e:
        return str(e), 500

    account = LinkedInAccount(
    company_name="ABC Company",
    access_token=encrypted_token,
    expires_at=expires_at
)
    add_to_database(account)

    return "LinkedIn account connected successfully."

if __name__ == "__main__":
    app.run(port=5000, debug=True)
