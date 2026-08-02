from time import monotonic
from uuid import UUID, uuid4

import structlog
from fastapi import Request, Response
from starlette.middleware.base import BaseHTTPMiddleware, RequestResponseEndpoint

from docmind_ai.errors import internal_error_response
from docmind_ai.request_context import bind_request_id, reset_request_id

logger = structlog.get_logger()


class RequestContextMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next: RequestResponseEndpoint) -> Response:
        request_id = self._resolve_request_id(request.headers.get("X-Request-ID"))
        context_token = bind_request_id(request_id)
        structlog.contextvars.clear_contextvars()
        structlog.contextvars.bind_contextvars(request_id=request_id)
        started = monotonic()
        status_code = 500
        try:
            try:
                response = await call_next(request)
            except Exception as exception:
                logger.error(
                    "ai_request_unhandled",
                    exception_type=exception.__class__.__name__,
                )
                response = internal_error_response()
            status_code = response.status_code
            response.headers["X-Request-ID"] = request_id
            return response
        finally:
            logger.info(
                "ai_request_completed",
                method=request.method,
                path=request.url.path,
                status=status_code,
                duration_ms=round((monotonic() - started) * 1000),
            )
            structlog.contextvars.clear_contextvars()
            reset_request_id(context_token)

    def _resolve_request_id(self, candidate: str | None) -> str:
        if candidate is None:
            return str(uuid4())
        try:
            UUID(candidate)
        except ValueError:
            return str(uuid4())
        return candidate
