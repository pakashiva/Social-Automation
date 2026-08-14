import traceback
from datetime import UTC, datetime
from zoneinfo import ZoneInfo

from flask import (
    Response,
    flash,
    get_flashed_messages,
    jsonify,
    request,
    stream_with_context,
)
from flask_jwt_extended import get_jwt_identity, jwt_required

from app import db
from initialize_database.models import CompanyInfo, ContentJob


VALID_PLATFORMS = {"linkedin", "instagram", "facebook"}


def register_generate_route(app):
    if "generate_content" in app.view_functions:
        return

    @app.route(
        "/generate_content",
        methods=["POST"],
        strict_slashes=False,
    )
    @jwt_required()
    def generate_content():
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


def register_schedule_route(app):
    if "schedule_content" in app.view_functions:
        return

    @app.route(
        "/schedule_content",
        methods=["POST"],
        strict_slashes=False,
    )
    @jwt_required()
    def schedule_content():
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
