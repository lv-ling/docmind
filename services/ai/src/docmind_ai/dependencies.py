from hmac import compare_digest
from typing import Annotated

from fastapi import Header, Request, status

from docmind_ai.config import Settings
from docmind_ai.contracts.common import ApiErrorCategory, ApiErrorCode
from docmind_ai.errors import AiServiceError


def settings_from_request(request: Request) -> Settings:
    settings = request.app.state.settings
    if not isinstance(settings, Settings):
        raise RuntimeError("Application settings are unavailable")
    return settings


def require_internal_token(
    request: Request,
    token: Annotated[str | None, Header(alias="X-DocMind-Internal-Token")] = None,
) -> None:
    expected = settings_from_request(request).internal_token.get_secret_value()
    if token is None or not compare_digest(token, expected):
        raise AiServiceError(
            status_code=status.HTTP_401_UNAUTHORIZED,
            code=ApiErrorCode.AUTHENTICATION_REQUIRED,
            category=ApiErrorCategory.AUTHENTICATION,
            safe_message="内部服务认证失败",
        )
