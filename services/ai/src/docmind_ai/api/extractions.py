from __future__ import annotations

from typing import Annotated

from fastapi import APIRouter, Depends, Request

from docmind_ai.contracts.common import ApiErrorCategory, ApiErrorCode
from docmind_ai.contracts.extraction import AiExtractionRequest, AiExtractionResponse
from docmind_ai.dependencies import require_internal_token
from docmind_ai.errors import AiServiceError
from docmind_ai.extraction import ExtractionWorkflow
from docmind_ai.extraction.workflow_errors import ExtractionWorkflowError

router = APIRouter(
    prefix="/internal/v1/extractions",
    tags=["extractions"],
    dependencies=[Depends(require_internal_token)],
)


def _workflow(request: Request) -> ExtractionWorkflow:
    return request.app.state.extraction_workflow  # type: ignore[no-any-return]


@router.post("/run", response_model=AiExtractionResponse)
async def run_extraction(
    payload: AiExtractionRequest,
    workflow: Annotated[ExtractionWorkflow, Depends(_workflow)],
) -> AiExtractionResponse:
    try:
        return await workflow.run(payload)
    except ExtractionWorkflowError as exception:
        raise AiServiceError(
            status_code=503 if exception.retryable else 422,
            code=(
                ApiErrorCode.DEPENDENCY_UNAVAILABLE
                if exception.retryable
                else ApiErrorCode.TASK_FAILED
            ),
            category=(
                ApiErrorCategory.DEPENDENCY if exception.retryable else ApiErrorCategory.TASK
            ),
            safe_message=exception.safe_message,
            details={"reason": exception.code, "retryable": exception.retryable},
        ) from exception
