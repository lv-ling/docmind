from uuid import uuid4

from fastapi.testclient import TestClient


def test_sensitive_endpoint_tokenizes_without_returning_originals(
    client: TestClient, internal_headers: dict[str, str]
) -> None:
    response = client.post(
        "/internal/v1/sensitive/tokenize",
        headers=internal_headers,
        json={
            "source_version_id": str(uuid4()),
            "language": "zh-CN",
            "country_codes": ["CN", "US", "JP", "KR", "DE", "FR", "GB", "AU", "NL"],
            "rules": [],
            "nodes": [
                {
                    "node_id": "paragraph-1",
                    "kind": "paragraph",
                    "page_number": 1,
                    "text": "邮箱 alice@example.com, 电话 +86 13800138000",
                    "metadata": {},
                }
            ],
        },
    )

    assert response.status_code == 200
    response_text = response.text
    assert "alice@example.com" not in response_text
    assert "13800138000" not in response_text
    assert "[[SENSITIVE:EMAIL_ADDRESS:01]]" in response_text
    assert "[[SENSITIVE:PHONE_NUMBER:01]]" in response_text


def test_sensitive_endpoint_requires_internal_authentication(client: TestClient) -> None:
    response = client.post(
        "/internal/v1/sensitive/tokenize",
        json={
            "source_version_id": str(uuid4()),
            "language": "en-US",
            "country_codes": ["US"],
            "rules": [],
            "nodes": [],
        },
    )

    assert response.status_code == 401
