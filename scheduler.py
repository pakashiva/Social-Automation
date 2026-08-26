from datetime import datetime, UTC, timedelta
from zoneinfo import ZoneInfo
import traceback
from apscheduler.schedulers.background import BackgroundScheduler
from croniter import croniter

from app import app, db

from services.notify_service import send_email

from initialize_database.models import (
    CompanyInfo,
    ContentJob,
    RecurringContent,
    User
)

from agents.topic_evaluator.eval_functions import generate_topic
from rag_system.rag_functions import retrieve_semantic_chunks
from agents.content_writer_agent.content_functions import (generate_linkedin_content , 
                                                           generate_facebook_content , 
                                                           generate_instagram_content)

from publisher.publisher_functions import (publish_to_facebook , 
                                          publish_to_instagram , 
                                          publish_to_linkedin)


# ============================================================
# CONFIGURATION
# ============================================================

# Number of future recurring posts we always want ready.
K = 3

# How often APScheduler checks for work.
CHECK_INTERVAL_SECONDS = 60


# ============================================================
# 1. GENERATE CONTENT
# ============================================================
def generate_content(user_id, platform):
    """
    Generate one platform-specific recurring post.

    This function only generates content.
    It does not save or publish anything.
    """

    output = generate_topic(
        user_id=user_id
    )

    data = retrieve_semantic_chunks(
        pillar=output.pillar,
        user_id=user_id
    )

    if platform == "linkedin":

        post = generate_linkedin_content(
            pillar=output.pillar,
            topic=output.topic,
            post_format=output.post_format,
            brand_voice=output.brand_voice,
            pillar_guidlines=data
        )

    elif platform == "facebook":

        # Replace this with your existing
        # Facebook content generation function.
        post = generate_facebook_content(
            pillar=output.pillar,
            topic=output.topic,
            post_format=output.post_format,
            brand_voice=output.brand_voice,
            pillar_guidlines=data
        )

    elif platform == "instagram":

        # Replace this with your existing
        # Instagram content generation function.
        post = generate_instagram_content(
            pillar=output.pillar,
            topic=output.topic,
            post_format=output.post_format,
            brand_voice=output.brand_voice,
            pillar_guidlines=data
        )

    else:
        raise ValueError(
            f"Unsupported platform: {platform}"
        )

    if hasattr(post, "content"):
        return post.content

    return str(post)


# ============================================================
# 2. GET NEXT RECURRING DATETIME
# ============================================================

def get_next_schedule(company, after_utc=None):
    """
    Calculate the next occurrence of the Company's cron schedule.

    CompanyInfo.scheduled_time:
        0 14 * * MON,WED,FRI

    CompanyInfo.timezone:
        Asia/Kolkata

    Returns the scheduled datetime in UTC.
    """

    user_timezone = ZoneInfo(company.timezone)

    if after_utc is None:
        after_utc = datetime.now(UTC)

    # Convert UTC -> user's timezone because the cron expression
    # represents the user's local schedule.
    local_time = after_utc.astimezone(user_timezone)

    cron = croniter(
        company.scheduled_time,
        local_time
    )

    next_local = cron.get_next(datetime)

    # Store actual post times in UTC.
    next_utc = next_local.astimezone(UTC)

    return next_utc


# ============================================================
# 3. CREATE ONE RECURRING POST
# ============================================================

def create_recurring_post(company, scheduled_at, platform):
    """
    Generate one platform-specific post
    and save it into RecurringContent.
    """

    print(
        f"Generating {platform} content "
        f"for user {company.user_id}"
    )

    content = generate_content(
        user_id=company.user_id,
        platform=platform
    )

    if not content:
        raise ValueError(
            f"Generated content is empty for {platform}"
        )

    recurring_post = RecurringContent(
        user_id=company.user_id,
        platform=platform,
        scheduled_at=scheduled_at,
        post_content=content,
        status="scheduled"
    )

    db.session.add(recurring_post)
    db.session.commit()

    print(
        f"Recurring {platform} post created for "
        f"{scheduled_at}"
    )

    return recurring_post


# ============================================================
# 4. MAINTAIN NEXT K RECURRING POSTS
# ============================================================

def maintain_recurring_posts(company):
    """
    Keep exactly K future recurring posts ready.

    K counts individual platform posts.

    Example:

        K = 3
        platforms = ["linkedin", "facebook", "instagram"]

        Result:

        Monday 10 AM     LinkedIn
        Monday 10 AM     Facebook
        Monday 10 AM     Instagram


        K = 3
        platforms = ["linkedin", "facebook"]

        Result:

        Monday 10 AM       LinkedIn
        Monday 10 AM       Facebook
        Wednesday 10 AM    LinkedIn
    """

    now_utc = datetime.now(UTC)

    # --------------------------------------------------------
    # Get selected platforms
    # --------------------------------------------------------

    platforms = company.platforms or []

    if not platforms:
        print(
            f"No platforms selected for user "
            f"{company.user_id}"
        )
        return

    # --------------------------------------------------------
    # Find existing future recurring posts
    # --------------------------------------------------------

    future_posts = (
        RecurringContent.query
        .filter(
            RecurringContent.user_id == company.user_id,
            RecurringContent.status == "scheduled",
            RecurringContent.scheduled_at > now_utc
        )
        .order_by(
            RecurringContent.scheduled_at.asc()
        )
        .all()
    )

    missing_posts = K - len(future_posts)

    if missing_posts <= 0:
        return

    print(
        f"User {company.user_id} needs "
        f"{missing_posts} recurring post(s)"
    )

    # --------------------------------------------------------
    # Determine where cron calculation starts
    # --------------------------------------------------------

    if future_posts:

        after_utc = future_posts[-1].scheduled_at

        if after_utc.tzinfo is None:
            after_utc = after_utc.replace(
                tzinfo=UTC
            )

    else:

        after_utc = now_utc

    # --------------------------------------------------------
    # Generate missing posts
    # --------------------------------------------------------

    generated = 0

    while generated < missing_posts:

        # Get the next recurring schedule time
        scheduled_at = get_next_schedule(
            company=company,
            after_utc=after_utc
        )

        # Generate one post for every selected
        # platform at this occurrence.
        for platform in platforms:

            if generated >= missing_posts:
                break

            create_recurring_post(
                company=company,
                scheduled_at=scheduled_at,
                platform=platform
            )

            generated += 1

        # Next cron calculation starts after
        # this recurring occurrence.
        after_utc = scheduled_at


        
# ============================================================
# 5. MAINTAIN RECURRING POSTS FOR ALL COMPANIES
# ============================================================

def maintain_all_recurring_posts():
    """
    Find every company that has a recurring schedule and ensure
    that each company has K future generated posts.
    """

    companies = (
        CompanyInfo.query
        .filter(
            CompanyInfo.scheduled_time.isnot(None)
        )
        .all()
    )

    for company in companies:

        try:

            maintain_recurring_posts(
                company
            )

        except Exception as e:

            db.session.rollback()

            print(
                f"Recurring generation failed for "
                f"user {company.user_id}: {e}"
            )


# ============================================================
# 6. PUBLISH DUE RECURRING POSTS
# ============================================================
def publish_due_recurring_posts():
    """
    Publish recurring posts whose scheduled time has arrived.
    Uses the publisher belonging to the post's platform.
    """

    now_utc = datetime.now(UTC)

    posts = (
        RecurringContent.query
        .filter(
            RecurringContent.status == "scheduled",
            RecurringContent.scheduled_at <= now_utc
        )
        .order_by(
            RecurringContent.scheduled_at.asc()
        )
        .all()
    )

    for post in posts:

        try:

            post.status = "publishing"
            db.session.commit()

            if post.platform == "facebook":

                publish_to_facebook(
                    message=post.post_content,
                    user_id=post.user_id
                )

            elif post.platform == "linkedin":

                publish_to_linkedin(
                    message=post.post_content,
                    user_id=post.user_id
                )

            elif post.platform == "instagram":

                publish_to_instagram(
                    message=post.post_content,
                    user_id=post.user_id
                )

            else:

                raise ValueError(
                    f"Unsupported platform: "
                    f"{post.platform}"
                )

            post.status = "published"
            db.session.commit()

            print(
                f"Recurring {post.platform} post "
                f"{post.id} published."
            )

        except Exception as e:

            db.session.rollback()

            post.status = "failed"
            db.session.commit()

            print(
                f"Recurring {post.platform} post "
                f"{post.id} failed: {e}"
            )

            traceback.print_exc()
# ============================================================
# 7. PUBLISH CUSTOM CONTENT JOBS
# ============================================================

def publish_due_content_jobs():
    """
    Publish custom posts created/scheduled by the user.

    These already contain final post_content, so NO AI content
    generation happens here.
    """

    now_utc = datetime.now(UTC)

    jobs = (
        ContentJob.query
        .filter(
            ContentJob.status == "scheduled",
            ContentJob.scheduled_at <= now_utc
        )
        .order_by(
            ContentJob.scheduled_at.asc()
        )
        .all()
    )

    print("Job recieved:" , jobs)

    for job in jobs:

        try:

            job.status = "publishing"
            db.session.commit()

            publish_to_facebook(
                message=job.post_content,
                user_id=job.user_id
            )

            job.status = "published"
            job.updated_at = datetime.now(UTC)

            db.session.commit()

            print(
                f"ContentJob {job.id} published."
            )

        except Exception as e:

            db.session.rollback()

            job.status = "failed"
            job.updated_at = datetime.now(UTC)

            db.session.commit()

            print(
                f"ContentJob {job.id} failed: {e}"
            )
            traceback.print_exc()


# ============================================================
# 8. NOTIFY SERVICES
# ============================================================

def send_post_notification(
    *,
    user,
    platform,
    scheduled_at,
    post_content,
):
    """
    Send the 1-hour-before-publishing notification.

    Email content is intentionally left to the application owner.
    """

    if not user or not user.email:
        raise ValueError(
            "User email is missing"
        )

    # You will provide the actual email content.
    subject = "Your scheduled post is coming up"

    body = """
    YOUR EMAIL CONTENT HERE
    """

    return send_email(
        recipient=user.email,
        subject=subject,
        body=body,
    )

def is_notification_due(scheduled_at, now):
    notification_time = (
        scheduled_at - timedelta(hours=1)
    )

    return (
        notification_time <= now
        and scheduled_at > now
    )

def notify_due_content_jobs():

    now_utc = datetime.now(UTC)

    notification_deadline = (
        now_utc + timedelta(hours=1)
    )

    jobs = (
        ContentJob.query
        .filter(
            ContentJob.status == "scheduled",

            ContentJob.scheduled_at.isnot(None),

            ContentJob.scheduled_at > now_utc,

            ContentJob.scheduled_at <= notification_deadline,

            ContentJob.email_notified_at.is_(None),
        )
        .order_by(
            ContentJob.scheduled_at.asc()
        )
        .all()
    )

    for job in jobs:

        try:

            user = User.query.filter_by(
                user_id=job.user_id
            ).first()

            if not user:
                print(
                    f"Notification skipped: "
                    f"user {job.user_id} not found"
                )
                continue

            send_post_notification(
                user=user,
                platform=job.platform,
                scheduled_at=job.scheduled_at,
                post_content=job.post_content,
            )

            job.email_notified_at = datetime.now(UTC)

            db.session.commit()

            print(
                f"Notification sent for ContentJob "
                f"{job.id}"
            )

        except Exception as exc:

            db.session.rollback()

            print(
                f"Notification failed for "
                f"ContentJob {job.id}: {exc}"
            )


def notify_due_recurring_posts():

    now_utc = datetime.now(UTC)

    notification_deadline = (
        now_utc + timedelta(hours=1)
    )

    posts = (
        RecurringContent.query
        .filter(
            RecurringContent.status == "scheduled",

            RecurringContent.scheduled_at > now_utc,

            RecurringContent.scheduled_at <= notification_deadline,

            RecurringContent.email_notified_at.is_(None),
        )
        .order_by(
            RecurringContent.scheduled_at.asc()
        )
        .all()
    )

    for post in posts:

        try:

            user = User.query.filter_by(
                user_id=post.user_id
            ).first()

            if not user:
                continue

            send_post_notification(
                user=user,
                platform=post.platform,
                scheduled_at=post.scheduled_at,
                post_content=post.post_content,
            )

            post.email_notified_at = datetime.now(UTC)

            db.session.commit()

            print(
                f"Notification sent for "
                f"RecurringContent {post.id}"
            )

        except Exception as exc:

            db.session.rollback()

            print(
                f"Notification failed for "
                f"RecurringContent {post.id}: {exc}"
            )

# ============================================================
# 9. MAIN SCHEDULER CYCLE
# ============================================================

def scheduler_cycle():
    """
    One complete scheduler cycle.

    Order:

    1. Publish recurring posts that are due.
    2. Publish custom posts that are due.
    3. Refill recurring posts until every company has K
       future posts.
    """

    with app.app_context():

        print(
            f"Scheduler check: {datetime.now(UTC)}"
        )

        notify_due_recurring_posts()

        notify_due_content_jobs()

        publish_due_recurring_posts()

        publish_due_content_jobs()

        maintain_all_recurring_posts()


# ============================================================
# 10. APSCHEDULER
# ============================================================

scheduler = BackgroundScheduler(
    timezone="UTC"
)


def start_scheduler():
    """
    Start APScheduler.

    Call this once when the Flask application starts.
    """

    if scheduler.running:
        return

    scheduler.add_job(
        scheduler_cycle,
        trigger="interval",
        seconds=CHECK_INTERVAL_SECONDS,
        id="social_content_scheduler",
        replace_existing=True,
        max_instances=1
    )

    scheduler.start()

    print(
        f"APScheduler started. "
        f"Checking every {CHECK_INTERVAL_SECONDS} seconds."
    )