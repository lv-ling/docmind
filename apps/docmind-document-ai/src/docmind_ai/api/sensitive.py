from __future__ import annotations

from typing import Annotated

from fastapi import APIRouter, Depends, Request
from starlette.concurrency import run_in_threadpool

from docmind_ai.config import Settings
from docmind_ai.contracts.common import ApiErrorCategory, ApiErrorCode
from docmind_ai.contracts.sensitive import (
    SensitiveTokenizationRequest,
    SensitiveTokenizationResponse,
)
from docmind_ai.dependencies import require_internal_token
from docmind_ai.errors import AiServiceError
from docmind_ai.pii import SensitiveTokenizer
from docmind_ai.pii.detector import SensitiveConfigurationError
from docmind_ai.pii.models import SensitiveLeakError

router = APIRouter(
    prefix="/internal/v1/sensitive",
    tags=["sensitive"],
    dependencies=[Depends(require_internal_token)],
)


def _tokenizer(request: Request) -> SensitiveTokenizer:
    return request.app.state.sensitive_tokenizer  # type: ignore[no-any-return]


@router.post("/tokenize", response_model=SensitiveTokenizationResponse)
async def tokenize_sensitive_text(
    request: Request,
    payload: SensitiveTokenizationRequest,
    tokenizer: Annotated[SensitiveTokenizer, Depends(_tokenizer)],
) -> SensitiveTokenizationResponse:
    settings: Settings = request.app.state.settings
    total_characters = sum(len(node.text) for node in payload.nodes)
    if total_characters > settings.max_sensitive_text_characters:
        raise AiServiceError(
            status_code=413,
            code=ApiErrorCode.VALIDATION_FAILED,
            category=ApiErrorCategory.VALIDATION,
            safe_message="待扫描文本超过安全限制",
            details={"max_characters": settings.max_sensitive_text_characters},
        )
    try:
        return await run_in_threadpool(tokenizer.tokenize, payload)
    except SensitiveConfigurationError as exception:
        raise AiServiceError(
            status_code=422,
            code=ApiErrorCode.VALIDATION_FAILED,
            category=ApiErrorCategory.VALIDATION,
            safe_message=exception.safe_message,
            details={"reason": "SENSITIVE_RULE_INVALID", "rule_key": exception.rule_key},
        ) from exception
    except SensitiveLeakError as exception:
        raise AiServiceError(
            status_code=422,
            code=ApiErrorCode.TASK_FAILED,
            category=ApiErrorCategory.TASK,
            safe_message=str(exception),
            details={"reason": "SENSITIVE_LEAK_DETECTED"},
        ) from exception
