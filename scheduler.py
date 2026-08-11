from datetime import datetime, timezone
from zoneinfo import ZoneInfo

from croniter import croniter

from app import app, db
from initialize_database.models import CompanyInfo

from agents.topic_evaluator.eval_functions import generate_topic
from rag_system.rag_functions import retrieve_semantic_chunks
from agents.content_writer_agent.content_functions import generate_linkedin_content
from publisher.facebook_functions import publish_to_facebook

import traceback


# ============================================================
# EXECUTE COMPLETE CONTENT PIPELINE
# ============================================================

def execute_scheduled_content(user_id):
    """
    Execute the complete content generation and publishing
    pipeline for one user.

    IMPORTANT:
    Any exception is allowed to propagate to the scheduler.
    This ensures failed executions are NOT marked as successful.
    """

    print()
    print("=" * 70)
    print(f"STARTING CONTENT PIPELINE FOR USER: {user_id}")
    print("=" * 70)

    # --------------------------------------------------------
    # STEP 1: Generate topic
    # --------------------------------------------------------

    print()
    print("[1/4] Calling generate_topic()...")

    output = generate_topic(user_id=user_id)

    if output is None:
        raise ValueError("generate_topic() returned None")

    print("[1/4] generate_topic() completed successfully")

    print("Pillar:", output.pillar)
    print("Topic:", output.topic)
    print("Post format:", output.post_format)
    print("Brand voice:", output.brand_voice)

    # --------------------------------------------------------
    # STEP 2: Retrieve semantic chunks
    # --------------------------------------------------------

    print()
    print("[2/4] Calling retrieve_semantic_chunks()...")

    data = retrieve_semantic_chunks(
        pillar=output.pillar,
        user_id=user_id
    )

    if data is None:
        raise ValueError(
            "retrieve_semantic_chunks() returned None"
        )

    print("[2/4] retrieve_semantic_chunks() completed successfully")

    print("Retrieved data length:", len(data))
    print("Retrieved data preview:")
    print(data[:200])

    # --------------------------------------------------------
    # STEP 3: Generate LinkedIn content
    # --------------------------------------------------------

    print()
    print("[3/4] Calling generate_linkedin_content()...")
    print("WARNING: If execution stops after this message,")
    print("the problem is most likely inside llm.invoke().")
    print()

    post = generate_linkedin_content(
        pillar=output.pillar,
        topic=output.topic,
        post_format=output.post_format,
        brand_voice=output.brand_voice,
        pillar_guidlines=data
    )

    if post is None:
        raise ValueError(
            "generate_linkedin_content() returned None"
        )

    print()
    print("[3/4] generate_linkedin_content() completed successfully")

    print("Generated content type:", type(post))

    # --------------------------------------------------------
    # Handle LangChain AIMessage
    # --------------------------------------------------------
    content = str(post)

    print("Generated content preview:")
    print(str(content)[:300])

    # --------------------------------------------------------
    # STEP 4: Publish to Facebook
    # --------------------------------------------------------

    print()
    print("[4/4] Calling publish_to_facebook()...")

    publish_result = publish_to_facebook(message=content)

    print()
    print("[4/4] publish_to_facebook() completed successfully")

    print("Facebook result:")
    print(publish_result)

    # --------------------------------------------------------
    # PIPELINE COMPLETE
    # --------------------------------------------------------

    print()
    print("=" * 70)
    print(f"CONTENT PIPELINE SUCCESSFUL FOR USER: {user_id}")
    print("=" * 70)
    print()

    return publish_result


# ============================================================
# GET SCHEDULED OCCURRENCES
# ============================================================

def get_scheduled_occurrences(
    cron_expression,
    timezone_name,
    now_utc,
    last_execution_utc=None
):
    """
    Return scheduled occurrences that have happened since the
    last execution.

    For the first execution, only the latest occurrence is returned.
    """

    user_timezone = ZoneInfo(timezone_name)

    now_local = now_utc.astimezone(user_timezone)

    print()
    print("Calculating scheduled occurrences...")
    print("Cron expression:", cron_expression)
    print("Timezone:", timezone_name)
    print("Current local time:", now_local)
    print("Last execution UTC:", last_execution_utc)

    # --------------------------------------------------------
    # If this company has executed before
    # --------------------------------------------------------

    if last_execution_utc:

        last_execution_local = (
            last_execution_utc
            .replace(tzinfo=timezone.utc)
            .astimezone(user_timezone)
        )

        print(
            "Last execution local time:",
            last_execution_local
        )

        cron = croniter(
            cron_expression,
            last_execution_local
        )

        occurrences = []

        while True:

            occurrence = cron.get_next(datetime)

            if occurrence > now_local:
                break

            occurrences.append(occurrence)

        print(
            f"Found {len(occurrences)} occurrence(s)"
        )

        return occurrences

    # --------------------------------------------------------
    # First-ever execution
    # --------------------------------------------------------

    print(
        "No previous execution found."
    )

    cron = croniter(
        cron_expression,
        now_local
    )

    occurrence = cron.get_prev(datetime)

    print(
        "Latest scheduled occurrence:",
        occurrence
    )

    return [occurrence]


# ============================================================
# PROCESS ONE COMPANY
# ============================================================

def process_company_schedule(
    company,
    now_utc
):
    """
    Find and execute all scheduled occurrences for one company.
    """

    print()
    print("-" * 70)
    print(
        f"PROCESSING COMPANY / USER: {company.user_id}"
    )
    print("-" * 70)

    # --------------------------------------------------------
    # No schedule configured
    # --------------------------------------------------------

    if not company.scheduled_time:

        print(
            f"No scheduled time for user {company.user_id}"
        )

        return

    print(
        "Scheduled time / cron:",
        company.scheduled_time
    )

    print(
        "Timezone:",
        company.timezone
    )

    print(
        "Last scheduled run:",
        company.last_scheduled_run_at
    )

    # --------------------------------------------------------
    # Find occurrences
    # --------------------------------------------------------

    occurrences = get_scheduled_occurrences(
        cron_expression=company.scheduled_time,
        timezone_name=company.timezone,
        now_utc=now_utc,
        last_execution_utc=company.last_scheduled_run_at
    )

    if not occurrences:

        print(
            f"No pending occurrences for "
            f"user {company.user_id}"
        )

        return

    print(
        f"Pending occurrences: {len(occurrences)}"
    )

    # --------------------------------------------------------
    # Execute each occurrence
    # --------------------------------------------------------

    for occurrence_local in occurrences:

        print()
        print("=" * 70)
        print(
            f"PROCESSING OCCURRENCE FOR USER "
            f"{company.user_id}"
        )
        print("=" * 70)

        # ----------------------------------------------------
        # Convert scheduled local time to UTC
        # ----------------------------------------------------

        occurrence_utc = occurrence_local.astimezone(
            timezone.utc
        )

        # SQLAlchemy DateTime column is timezone-naive
        occurrence_utc_naive = (
            occurrence_utc.replace(tzinfo=None)
        )

        print(
            "Scheduled occurrence local:",
            occurrence_local
        )

        print(
            "Scheduled occurrence UTC:",
            occurrence_utc
        )

        print(
            "Scheduled occurrence naive UTC:",
            occurrence_utc_naive
        )

        # ----------------------------------------------------
        # Prevent duplicate execution
        # ----------------------------------------------------

        if (
            company.last_scheduled_run_at
            and occurrence_utc_naive
            <= company.last_scheduled_run_at
        ):

            print(
                "Occurrence already executed."
            )

            print(
                "Skipping..."
            )

            continue

        print()
        print(
            f"Executing user {company.user_id}"
        )

        # ----------------------------------------------------
        # Execute COMPLETE pipeline
        # ----------------------------------------------------

        try:

            print()
            print(
                ">>> BEGINNING execute_scheduled_content()"
            )

            result = execute_scheduled_content(
                company.user_id
            )

            print()
            print(
                ">>> execute_scheduled_content() RETURNED"
            )

            print(
                "Result:",
                result
            )

            # ------------------------------------------------
            # ONLY mark as executed AFTER success
            # ------------------------------------------------

            company.last_scheduled_run_at = (
                occurrence_utc_naive
            )

            db.session.commit()

            print()
            print("=" * 70)
            print(
                f"SUCCESSFULLY EXECUTED USER "
                f"{company.user_id}"
            )
            print(
                "last_scheduled_run_at updated to:",
                occurrence_utc_naive
            )
            print("=" * 70)

        except Exception as e:

            # ------------------------------------------------
            # Rollback database changes
            # ------------------------------------------------

            db.session.rollback()

            print()
            print("=" * 70)
            print(
                f"FAILED TO EXECUTE USER "
                f"{company.user_id}"
            )
            print("=" * 70)

            print(
                "Exception type:",
                type(e).__name__
            )

            print(
                "Exception:",
                str(e)
            )

            print()
            print("FULL TRACEBACK:")
            traceback.print_exc()

            print()
            print(
                "last_scheduled_run_at was NOT updated."
            )

            print(
                "The next GitHub Actions run can retry "
                "this occurrence."
            )

            print("=" * 70)

            # IMPORTANT:
            # Do NOT update last_scheduled_run_at here.

            # Continue processing other companies.
            continue


# ============================================================
# CHECK ALL SCHEDULES
# ============================================================

def check_all_schedules():

    now_utc = datetime.now(timezone.utc)

    print()
    print("=" * 70)
    print("SCHEDULER STARTED")
    print("=" * 70)

    print(
        "Current UTC time:",
        now_utc
    )

    print("=" * 70)

    # --------------------------------------------------------
    # Get all companies that have a schedule
    # --------------------------------------------------------

    companies = CompanyInfo.query.filter(
        CompanyInfo.scheduled_time.isnot(None)
    ).all()

    print()
    print(
        f"Found {len(companies)} scheduled companies"
    )

    # --------------------------------------------------------
    # Process every company
    # --------------------------------------------------------

    for company in companies:

        print()
        print(
            "#" * 70
        )

        print(
            f"Starting company: {company.user_id}"
        )

        print(
            "#" * 70
        )

        try:

            process_company_schedule(
                company,
                now_utc
            )

        except Exception as e:

            db.session.rollback()

            print()
            print(
                f"ERROR PROCESSING USER "
                f"{company.user_id}"
            )

            print(
                "Exception type:",
                type(e).__name__
            )

            print(
                "Exception:",
                str(e)
            )

            print()
            print("FULL TRACEBACK:")

            traceback.print_exc()

            # Continue with next company
            continue

    print()
    print("=" * 70)
    print("SCHEDULER FINISHED")
    print("=" * 70)
    print()


# ============================================================
# MAIN ENTRY POINT
# ============================================================

if __name__ == "__main__":

    print()
    print("=" * 70)
    print("STARTING SCHEDULER APPLICATION")
    print("=" * 70)

    try:

        with app.app_context():

            check_all_schedules()

    except Exception as e:

        print()
        print("=" * 70)
        print("FATAL SCHEDULER ERROR")
        print("=" * 70)

        print(
            "Exception type:",
            type(e).__name__
        )

        print(
            "Exception:",
            str(e)
        )

        print()
        print("FULL TRACEBACK:")

        traceback.print_exc()

        print("=" * 70)

        # Make GitHub Actions recognize this as a failed job
        raise

    finally:

        print()
        print("=" * 70)
        print("SCHEDULER PROCESS EXITING")
        print("=" * 70)