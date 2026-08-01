from uuid import UUID

from fastapi import FastAPI
from fastapi.testclient import TestClient

from docmind_ai.config import Settings


def test_health_preserves_valid_request_id(client: TestClient) -> None:
    request_id = "1de60124-f1c8-49aa-b34b-113d4347f516"

    response = client.get("/health/live", headers={"X-Request-ID": request_id})

    assert response.status_code == 200
    assert response.headers["X-Request-ID"] == request_id
    assert response.json() == {"status": "up", "service": "docmind-ai", "version": "0.1.0"}


def test_health_replaces_invalid_request_id(client: TestClient) -> None:
    response = client.get("/health/ready", headers={"X-Request-ID": "not-a-uuid"})

    assert response.status_code == 200
    UUID(response.headers["X-Request-ID"])


def test_internal_endpoint_requires_constant_time_service_token(
    client: TestClient, settings: Settings
) -> None:
    denied = client.get("/internal/v1/capabilities")

    assert denied.status_code == 401
    assert denied.json()["code"] == "AUTHENTICATION_REQUIRED"
    assert "internal_token" not in denied.text

    allowed = client.get(
        "/internal/v1/capabilities",
        headers={"X-DocMind-Internal-Token": settings.internal_token.get_secret_value()},
    )

    assert allowed.status_code == 200
    assert allowed.json()["country_codes"] == [
        "CN",
        "US",
        "JP",
        "KR",
        "DE",
        "FR",
        "GB",
        "AU",
        "NL",
    ]


def test_unhandled_exception_returns_safe_error(client: TestClient) -> None:
    app: FastAPI = client.app  # type: ignore[assignment]

    @app.get("/test-only/failure")
    async def fail_safely() -> None:
        raise RuntimeError("document body must never be exposed")

    response = client.get("/test-only/failure")

    assert response.status_code == 500
    assert response.json()["code"] == "INTERNAL_ERROR"
    assert "document body" not in response.text


def test_validation_error_uses_shared_shape(client: TestClient) -> None:
    app: FastAPI = client.app  # type: ignore[assignment]

    @app.get("/test-only/validate/{count}")
    async def validate(count: int) -> dict[str, int]:
        return {"count": count}

    response = client.get("/test-only/validate/not-an-integer")

    assert response.status_code == 422
    payload = response.json()
    assert payload["code"] == "VALIDATION_FAILED"
    assert payload["field_errors"][0]["path"] == "path.count"
    assert "not-an-integer" not in response.text
