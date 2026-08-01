from datetime import UTC, datetime
from typing import Any
from uuid import UUID

from fastapi import Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from docmind_ai.contracts.common import (
    ApiErrorCategory,
    ApiErrorCode,
    ApiErrorResponse,
    ApiFieldError,
)
from docmind_ai.request_context import current_request_id


class AiServiceError(Exception):
    def __init__(
        self,
        *,
        status_code: int,
        code: ApiErrorCode,
        category: ApiErrorCategory,
        safe_message: str,
        details: dict[str, Any] | None = None,
    ) -> None:
        super().__init__(safe_message)
        self.status_code = status_code
        self.code = code
        self.category = category
        self.safe_message = safe_message
        self.details = details or {}


def error_response(
    *,
    status_code: int,
    code: ApiErrorCode,
    category: ApiErrorCategory,
    message: str,
    details: dict[str, Any] | None = None,
    field_errors: list[ApiFieldError] | None = None,
) -> JSONResponse:
    payload = ApiErrorResponse(
        code=code,
        category=category,
        message=message,
        details=details or {},
        field_errors=field_errors or [],
        request_id=UUID(current_request_id()),
        timestamp=datetime.now(UTC),
    )
    return JSONResponse(status_code=status_code, content=payload.model_dump(mode="json"))


async def service_error_handler(_request: Request, exception: AiServiceError) -> JSONResponse:
    return error_response(
        status_code=exception.status_code,
        code=exception.code,
        category=exception.category,
        message=exception.safe_message,
        details=exception.details,
    )


async def validation_error_handler(
    _request: Request, exception: RequestValidationError
) -> JSONResponse:
    field_errors = [
        ApiFieldError(
            path=".".join(str(part) for part in error["loc"]),
            code=str(error["type"]),
            message="字段值无效",
        )
        for error in exception.errors()
    ]
    return error_response(
        status_code=422,
        code=ApiErrorCode.VALIDATION_FAILED,
        category=ApiErrorCategory.VALIDATION,
        message="请求参数无效",
        field_errors=field_errors,
    )


def internal_error_response() -> JSONResponse:
    return error_response(
        status_code=500,
        code=ApiErrorCode.INTERNAL_ERROR,
        category=ApiErrorCategory.INTERNAL,
        message="服务暂时不可用",
    )
