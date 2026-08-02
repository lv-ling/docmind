from __future__ import annotations

from typing import Annotated
from uuid import UUID

from fastapi import APIRouter, Depends, File, Form, Request, UploadFile
from starlette.concurrency import run_in_threadpool

from docmind_ai.config import Settings
from docmind_ai.contracts.common import ApiErrorCategory, ApiErrorCode
from docmind_ai.contracts.document import DocumentFormat, ParseDocumentResponse
from docmind_ai.dependencies import require_internal_token
from docmind_ai.errors import AiServiceError
from docmind_ai.parsing import DocumentParsingService
from docmind_ai.parsing.common import DocumentParsingError

router = APIRouter(
    prefix="/internal/v1/documents",
    tags=["documents"],
    dependencies=[Depends(require_internal_token)],
)


def _parsing_service(request: Request) -> DocumentParsingService:
    return request.app.state.parsing_service  # type: ignore[no-any-return]


@router.post("/parse", response_model=ParseDocumentResponse)
async def parse_document(
    request: Request,
    source_version_id: Annotated[UUID, Form()],
    source_format: Annotated[DocumentFormat, Form()],
    language: Annotated[str, Form(min_length=1, max_length=35)],
    file: Annotated[UploadFile, File()],
    parsing_service: Annotated[DocumentParsingService, Depends(_parsing_service)],
) -> ParseDocumentResponse:
    settings: Settings = request.app.state.settings
    content = await _read_bounded_upload(file, settings.max_document_bytes)
    _validate_signature(source_format, content)
    try:
        return await run_in_threadpool(
            parsing_service.parse,
            source_version_id=source_version_id,
            source_format=source_format,
            content=content,
            language=language,
        )
    except DocumentParsingError as exception:
        dependency_error = exception.code in {
            "LIBREOFFICE_UNAVAILABLE",
            "DOC_CONVERSION_TIMEOUT",
        }
        raise AiServiceError(
            status_code=503 if dependency_error else 422,
            code=(
                ApiErrorCode.DEPENDENCY_UNAVAILABLE
                if dependency_error
                else ApiErrorCode.VALIDATION_FAILED
            ),
            category=(
                ApiErrorCategory.DEPENDENCY if dependency_error else ApiErrorCategory.VALIDATION
            ),
            safe_message=exception.safe_message,
            details={"reason": exception.code},
        ) from exception


async def _read_bounded_upload(file: UploadFile, max_bytes: int) -> bytes:
    chunks: list[bytes] = []
    total = 0
    try:
        while chunk := await file.read(64 * 1024):
            total += len(chunk)
            if total > max_bytes:
                raise AiServiceError(
                    status_code=413,
                    code=ApiErrorCode.VALIDATION_FAILED,
                    category=ApiErrorCategory.VALIDATION,
                    safe_message="文档超过大小限制",
                    details={"max_bytes": max_bytes},
                )
            chunks.append(chunk)
    finally:
        await file.close()
    if total == 0:
        raise AiServiceError(
            status_code=422,
            code=ApiErrorCode.VALIDATION_FAILED,
            category=ApiErrorCategory.VALIDATION,
            safe_message="文档不能为空",
            details={"reason": "EMPTY_DOCUMENT"},
        )
    return b"".join(chunks)


def _validate_signature(source_format: DocumentFormat, content: bytes) -> None:
    matches = {
        "doc": content.startswith(bytes.fromhex("D0CF11E0A1B11AE1")),
        "docx": content.startswith(b"PK\x03\x04"),
        "pdf": content.lstrip().startswith(b"%PDF-"),
    }
    if not matches[source_format]:
        raise AiServiceError(
            status_code=422,
            code=ApiErrorCode.VALIDATION_FAILED,
            category=ApiErrorCategory.VALIDATION,
            safe_message="文件内容与声明格式不一致",
            details={"reason": "FILE_SIGNATURE_MISMATCH"},
        )
