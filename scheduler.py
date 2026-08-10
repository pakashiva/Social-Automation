from datetime import datetime, timedelta, timezone
from zoneinfo import ZoneInfo

from croniter import croniter

from app import app, db
from initialize_database.models import CompanyInfo

from agents.topic_evaluator.eval_functions import generate_topic
from rag_system.rag_functions import retrieve_semantic_chunks
from agents.content_writer_agent.content_functions import generate_linkedin_content
from publisher.facebook_functions import publish_to_facebook



def execute_scheduled_content(user_id):
    """
    Execute the complete content generation
    and publishing pipeline for one user.
    """
    try:
        output = generate_topic(user_id=user_id)
        data = retrieve_semantic_chunks(pillar=output.pillar , user_id=user_id)
        post = generate_linkedin_content(pillar=output.pillar , topic=output.topic , 
                                    post_format=output.post_format , brand_voice=output.brand_voice,
                                    pillar_guidlines=data)

        publish_to_facebook(post.content)
    except Exception as e:
        return str(e)


def get_scheduled_occurrences(
    cron_expression,
    timezone_name,
    now_utc
):
    """
    Return all cron occurrences that happened
    during the previous one-hour window.
    """

    user_timezone = ZoneInfo(timezone_name)

    # Convert current UTC time to user's timezone
    now_local = now_utc.astimezone(user_timezone)

    # One-hour window
    window_start_local = (
        now_local - timedelta(hours=1)
    )

    # Create cron iterator using the user's
    # local time as the reference
    cron = croniter(
        cron_expression,
        now_local
    )

    occurrences = []

    while True:

        occurrence = cron.get_prev(datetime)

        # Outside our one-hour window
        if occurrence < window_start_local:
            break

        # Do not process future occurrences
        if occurrence > now_local:
            continue

        occurrences.append(occurrence)

    # croniter returns newest → oldest.
    # Reverse so execution happens chronologically.
    occurrences.reverse()

    return occurrences

def process_company_schedule(
    company,
    now_utc
):
    """
    Find and execute all scheduled occurrences
    for one company during the previous hour.
    """

    if not company.scheduled_time:
        return

    occurrences = get_scheduled_occurrences(
        cron_expression=company.scheduled_time,
        timezone_name=company.timezone,
        now_utc=now_utc
    )

    if not occurrences:
        return

    for occurrence_local in occurrences:

        # Convert scheduled occurrence to UTC
        occurrence_utc = occurrence_local.astimezone(
            timezone.utc
        )

        # Convert to naive UTC because your SQLAlchemy
        # DateTime column is currently timezone-naive.
        occurrence_utc_naive = (
            occurrence_utc.replace(tzinfo=None)
        )

        # Already executed?
        if (
            company.last_scheduled_run_at
            and occurrence_utc_naive
            <= company.last_scheduled_run_at
        ):
            continue

        print(
            f"Executing user {company.user_id}"
        )
        print(
            f"Scheduled time: {occurrence_local}"
        )
        print(
            f"UTC time: {occurrence_utc}"
        )

        try:

            execute_scheduled_content(
                company.user_id
            )

            # Only mark the occurrence as executed
            # AFTER successful execution.
            company.last_scheduled_run_at = (
                occurrence_utc_naive
            )

            db.session.commit()

            print(
                f"Successfully executed "
                f"{company.user_id}"
            )

        except Exception as e:

            db.session.rollback()

            print(
                f"Failed to execute "
                f"{company.user_id}: {e}"
            )

            # Don't update last_scheduled_run_at.
            #
            # This allows the next GitHub Action run
            # to retry the failed occurrence.


def check_all_schedules():

    now_utc = datetime.now(timezone.utc)

    print("=" * 60)
    print("Scheduler started")
    print(f"Current UTC time: {now_utc}")
    print("=" * 60)

    companies = CompanyInfo.query.filter(
        CompanyInfo.scheduled_time.isnot(None)
    ).all()

    print(
        f"Found {len(companies)} scheduled companies"
    )

    for company in companies:

        try:

            process_company_schedule(
                company,
                now_utc
            )

        except Exception as e:

            print(
                f"Error processing user "
                f"{company.user_id}: {e}"
            )

    print("=" * 60)
    print("Scheduler finished")
    print("=" * 60)

if __name__ == "__main__":

    with app.app_context():

        check_all_schedules()