import pytest
from pydantic import SecretStr, ValidationError

from docmind_ai.config import Settings


def test_rejects_short_internal_token() -> None:
    with pytest.raises(ValidationError):
        Settings(environment="test", internal_token=SecretStr("too-short"))


def test_rejects_local_token_in_production() -> None:
    with pytest.raises(ValidationError):
        Settings(
            environment="production",
            internal_token=SecretStr(
                "local-only-docmind-ai-internal-token-change-before-production"
            ),
        )
