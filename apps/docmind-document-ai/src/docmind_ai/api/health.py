from fastapi import APIRouter, Request

from docmind_ai.config import Settings
from docmind_ai.contracts.common import HealthResponse
from docmind_ai.dependencies import settings_from_request

router = APIRouter(tags=["health"])


def _health(settings: Settings) -> HealthResponse:
    return HealthResponse(
        status="up",
        service=settings.service_name,
        version=settings.service_version,
    )


@router.get("/health/live", response_model=HealthResponse)
async def liveness(request: Request) -> HealthResponse:
    return _health(settings_from_request(request))


@router.get("/health/ready", response_model=HealthResponse)
async def readiness(request: Request) -> HealthResponse:
    return _health(settings_from_request(request))
