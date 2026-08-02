from datetime import datetime
from enum import StrEnum
from typing import Any
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field


class StrictModel(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)


class ApiErrorCategory(StrEnum):
    AUTHENTICATION = "authentication"
    VALIDATION = "validation"
    DEPENDENCY = "dependency"
    TASK = "task"
    INTERNAL = "internal"


class ApiErrorCode(StrEnum):
    AUTHENTICATION_REQUIRED = "AUTHENTICATION_REQUIRED"
    VALIDATION_FAILED = "VALIDATION_FAILED"
    DEPENDENCY_UNAVAILABLE = "DEPENDENCY_UNAVAILABLE"
    TASK_FAILED = "TASK_FAILED"
    INTERNAL_ERROR = "INTERNAL_ERROR"


class ApiFieldError(StrictModel):
    path: str
    code: str
    message: str


class ApiErrorResponse(StrictModel):
    code: ApiErrorCode
    category: ApiErrorCategory
    message: str
    details: dict[str, Any] = Field(default_factory=dict)
    field_errors: list[ApiFieldError] = Field(default_factory=list)
    request_id: UUID
    timestamp: datetime


class HealthResponse(StrictModel):
    status: str
    service: str
    version: str
