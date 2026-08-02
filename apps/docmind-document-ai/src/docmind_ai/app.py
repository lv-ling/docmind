from fastapi import FastAPI
from fastapi.exceptions import RequestValidationError

from docmind_ai.api import (
    capabilities_router,
    documents_router,
    extractions_router,
    health_router,
    sensitive_router,
)
from docmind_ai.config import Settings, load_settings
from docmind_ai.errors import AiServiceError, service_error_handler, validation_error_handler
from docmind_ai.extraction import ExtractionWorkflow, MockExtractionProvider
from docmind_ai.logging import configure_logging
from docmind_ai.middleware import RequestContextMiddleware
from docmind_ai.parsing import DocumentParsingService
from docmind_ai.pii import SensitiveTokenizer


def create_app(settings: Settings | None = None) -> FastAPI:
    configured = settings or load_settings()
    configure_logging(configured.log_level)
    app = FastAPI(
        title="DocMind AI",
        version=configured.service_version,
        docs_url="/docs" if configured.docs_enabled else None,
        redoc_url=None,
        openapi_url="/openapi.json" if configured.docs_enabled else None,
    )
    app.state.settings = configured
    app.state.parsing_service = DocumentParsingService(
        libreoffice_binary=configured.libreoffice_binary,
        conversion_timeout_seconds=configured.conversion_timeout_seconds,
        max_output_bytes=configured.max_conversion_output_bytes,
        max_pdf_pages=configured.max_pdf_pages,
    )
    app.state.sensitive_tokenizer = SensitiveTokenizer()
    app.state.extraction_workflow = ExtractionWorkflow(
        provider=MockExtractionProvider(),
        provider_timeout_seconds=configured.extraction_provider_timeout_seconds,
        total_timeout_seconds=configured.extraction_total_timeout_seconds,
        retry_attempts=configured.extraction_retry_attempts,
        retry_backoff_seconds=configured.extraction_retry_backoff_seconds,
        input_token_budget=configured.extraction_input_token_budget,
        output_token_budget=configured.extraction_output_token_budget,
    )
    app.add_middleware(RequestContextMiddleware)
    app.add_exception_handler(AiServiceError, service_error_handler)  # type: ignore[arg-type]
    app.add_exception_handler(RequestValidationError, validation_error_handler)  # type: ignore[arg-type]
    app.include_router(health_router)
    app.include_router(capabilities_router)
    app.include_router(documents_router)
    app.include_router(sensitive_router)
    app.include_router(extractions_router)
    return app
