from collections.abc import Iterator

import pytest
from fastapi.testclient import TestClient
from pydantic import SecretStr

from docmind_ai.app import create_app
from docmind_ai.config import Settings


@pytest.fixture
def settings() -> Settings:
    return Settings(
        environment="test",
        internal_token=SecretStr("test-only-internal-token-with-at-least-32-characters"),
        docs_enabled=False,
    )


@pytest.fixture
def client(settings: Settings) -> Iterator[TestClient]:
    with TestClient(create_app(settings), raise_server_exceptions=False) as test_client:
        yield test_client


@pytest.fixture
def internal_headers(settings: Settings) -> dict[str, str]:
    return {"X-DocMind-Internal-Token": settings.internal_token.get_secret_value()}
