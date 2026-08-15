from datetime import datetime, UTC
from zoneinfo import ZoneInfo

from apscheduler.schedulers.background import BackgroundScheduler
from croniter import croniter

from app import app, db

from initialize_database.models import (
    CompanyInfo,
    ContentJob,
    RecurringContent,
)

from agents.topic_evaluator.eval_functions import generate_topic
from rag_system.rag_functions import retrieve_semantic_chunks
from agents.content_writer_agent.content_functions import generate_linkedin_content

from publisher.facebook_functions import publish_to_facebook


# ============================================================
# CONFIGURATION
# ============================================================

# Number of future recurring posts we always want ready.
K = 2

# How often APScheduler checks for work.
CHECK_INTERVAL_SECONDS = 60


# ============================================================
# 1. GENERATE CONTENT
# ============================================================

def generate_content(user_id):
    """
    Generate one recurring post for a user.

    This function ONLY generates content.
    It does not save to the database.
    It does not publish anything.
    """

    output = generate_topic(
        user_id=user_id
    )

    data = retrieve_semantic_chunks(
        pillar=output.pillar,
        user_id=user_id
    )

    post = generate_linkedin_content(
        pillar=output.pillar,
        topic=output.topic,
        post_format=output.post_format,
        brand_voice=output.brand_voice,
        pillar_guidlines=data
    )

    # Some LLM libraries return an AIMessage.
    # Others may already return a string.
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

def create_recurring_post(company, scheduled_at):
    """
    Generate one post and save it into RecurringContent.

    This does NOT publish the post.
    """

    print(
        f"Generating recurring content for user "
        f"{company.user_id}"
    )

    content = generate_content(
        user_id=company.user_id
    )

    recurring_post = RecurringContent(
        user_id=company.user_id,
        platform="facebook",
        scheduled_at=scheduled_at,
        post_content=content,
        status="scheduled"
    )

    db.session.add(recurring_post)
    db.session.commit()

    print(
        f"Recurring content created for "
        f"{scheduled_at}"
    )

    return recurring_post


# ============================================================
# 4. MAINTAIN NEXT K RECURRING POSTS
# ============================================================

def maintain_recurring_posts(company):
    """
    Make sure this company always has K future recurring posts.

    Example:

        K = 2

        Existing:
            Monday
            Wednesday

        Nothing happens.

        After Monday is published:

        Existing future:
            Wednesday

        Only 1 exists.

        This function generates:
            Friday

        Future posts are again:
            Wednesday
            Friday
    """

    now_utc = datetime.now(UTC)

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
    # Determine where cron calculation should start
    # --------------------------------------------------------

    if future_posts:
        after_utc = future_posts[-1].scheduled_at

        # SQLite/Postgres behaviour can sometimes return
        # a naive datetime depending on configuration.
        if after_utc.tzinfo is None:
            after_utc = after_utc.replace(
                tzinfo=UTC
            )

    else:
        after_utc = now_utc

    # --------------------------------------------------------
    # Generate missing posts
    # --------------------------------------------------------

    for _ in range(missing_posts):

        scheduled_at = get_next_schedule(
            company=company,
            after_utc=after_utc
        )

        create_recurring_post(
            company=company,
            scheduled_at=scheduled_at
        )

        # Next cron calculation starts after this post.
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
    Publish generated recurring posts whose scheduled time
    has arrived.
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

            # Mark first so this post is not selected again.
            post.status = "publishing"
            db.session.commit()

            publish_to_facebook(
                message=post.post_content,
                user_id=post.user_id
            )

            post.status = "published"
            db.session.commit()

            print(
                f"Recurring post {post.id} published."
            )

        except Exception as e:

            db.session.rollback()

            post.status = "failed"
            db.session.commit()

            print(
                f"Recurring post {post.id} failed: {e}"
            )


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


# ============================================================
# 8. MAIN SCHEDULER CYCLE
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

        publish_due_recurring_posts()

        publish_due_content_jobs()

        maintain_all_recurring_posts()


# ============================================================
# 9. APSCHEDULER
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