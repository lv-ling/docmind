import os
from functools import lru_cache
from typing import Literal, Self

from pydantic import BaseModel, ConfigDict, Field, SecretStr, TypeAdapter, model_validator

Environment = Literal["local", "test", "staging", "production"]
LogLevel = Literal["DEBUG", "INFO", "WARNING", "ERROR", "CRITICAL"]

_environment_adapter: TypeAdapter[Environment] = TypeAdapter(Environment)
_log_level_adapter: TypeAdapter[LogLevel] = TypeAdapter(LogLevel)


class Settings(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    service_name: str = "docmind-document-ai"
    service_version: str = "0.1.0"
    environment: Environment = "local"
    host: str = "127.0.0.1"
    port: int = Field(default=8090, ge=1, le=65535)
    log_level: LogLevel = "INFO"
    internal_token: SecretStr = SecretStr(
        "local-only-docmind-document-ai-token-change-before-production"
    )
    docs_enabled: bool = True
    max_document_bytes: int = Field(default=10 * 1024 * 1024, ge=1, le=50 * 1024 * 1024)
    max_pdf_pages: int = Field(default=500, ge=1, le=5_000)
    libreoffice_binary: str = Field(default="libreoffice", min_length=1, max_length=500)
    conversion_timeout_seconds: float = Field(default=30, gt=0, le=300)
    max_conversion_output_bytes: int = Field(default=25 * 1024 * 1024, ge=1, le=100 * 1024 * 1024)
    max_sensitive_text_characters: int = Field(default=5_000_000, ge=1, le=20_000_000)
    extraction_provider_timeout_seconds: float = Field(default=30, gt=0, le=300)
    extraction_total_timeout_seconds: float = Field(default=45, gt=0, le=600)
    extraction_retry_attempts: int = Field(default=2, ge=0, le=5)
    extraction_retry_backoff_seconds: float = Field(default=0.05, ge=0, le=10)
    extraction_input_token_budget: int = Field(default=50_000, ge=100, le=1_000_000)
    extraction_output_token_budget: int = Field(default=10_000, ge=100, le=200_000)

    @model_validator(mode="after")
    def reject_unsafe_production_defaults(self) -> Self:
        token = self.internal_token.get_secret_value()
        if len(token) < 32:
            raise ValueError("internal service token must contain at least 32 characters")
        if self.environment == "production" and token.startswith("local-only-"):
            raise ValueError("local internal service token is forbidden in production")
        return self

    @classmethod
    def from_environment(cls) -> Self:
        return cls(
            service_name=os.getenv("DOCMIND_AI_SERVICE_NAME", "docmind-document-ai"),
            service_version=os.getenv("DOCMIND_AI_SERVICE_VERSION", "0.1.0"),
            environment=_environment_adapter.validate_python(
                os.getenv("DOCMIND_AI_ENVIRONMENT", "local")
            ),
            host=os.getenv("DOCMIND_AI_HOST", "127.0.0.1"),
            port=int(os.getenv("DOCMIND_AI_PORT", "8090")),
            log_level=_log_level_adapter.validate_python(os.getenv("DOCMIND_AI_LOG_LEVEL", "INFO")),
            internal_token=SecretStr(
                os.getenv(
                    "DOCMIND_AI_INTERNAL_TOKEN",
                    "local-only-docmind-document-ai-token-change-before-production",
                )
            ),
            docs_enabled=os.getenv("DOCMIND_AI_DOCS_ENABLED", "true").lower()
            in {"1", "true", "yes", "on"},
            max_document_bytes=int(
                os.getenv("DOCMIND_AI_MAX_DOCUMENT_BYTES", str(10 * 1024 * 1024))
            ),
            max_pdf_pages=int(os.getenv("DOCMIND_AI_MAX_PDF_PAGES", "500")),
            libreoffice_binary=os.getenv("DOCMIND_AI_LIBREOFFICE_BINARY", "libreoffice"),
            conversion_timeout_seconds=float(
                os.getenv("DOCMIND_AI_CONVERSION_TIMEOUT_SECONDS", "30")
            ),
            max_conversion_output_bytes=int(
                os.getenv("DOCMIND_AI_MAX_CONVERSION_OUTPUT_BYTES", str(25 * 1024 * 1024))
            ),
            max_sensitive_text_characters=int(
                os.getenv("DOCMIND_AI_MAX_SENSITIVE_TEXT_CHARACTERS", "5000000")
            ),
            extraction_provider_timeout_seconds=float(
                os.getenv("DOCMIND_AI_EXTRACTION_PROVIDER_TIMEOUT_SECONDS", "30")
            ),
            extraction_total_timeout_seconds=float(
                os.getenv("DOCMIND_AI_EXTRACTION_TOTAL_TIMEOUT_SECONDS", "45")
            ),
            extraction_retry_attempts=int(os.getenv("DOCMIND_AI_EXTRACTION_RETRY_ATTEMPTS", "2")),
            extraction_retry_backoff_seconds=float(
                os.getenv("DOCMIND_AI_EXTRACTION_RETRY_BACKOFF_SECONDS", "0.05")
            ),
            extraction_input_token_budget=int(
                os.getenv("DOCMIND_AI_EXTRACTION_INPUT_TOKEN_BUDGET", "50000")
            ),
            extraction_output_token_budget=int(
                os.getenv("DOCMIND_AI_EXTRACTION_OUTPUT_TOKEN_BUDGET", "10000")
            ),
        )


@lru_cache(maxsize=1)
def load_settings() -> Settings:
    return Settings.from_environment()
