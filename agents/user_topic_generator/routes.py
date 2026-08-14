import traceback

from flask import Response, jsonify, request, stream_with_context
from flask_jwt_extended import get_jwt_identity, jwt_required

from initialize_database.models import CompanyInfo


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
        valid_platforms = {"linkedin", "instagram", "facebook"}

        if content_source not in valid_sources:
            return jsonify({
                "error": "Please choose a valid content source."
            }), 400

        if platform not in valid_platforms:
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
