from uuid import uuid4

from fastapi.testclient import TestClient

from test_document_parsing import build_docx


def test_parse_endpoint_requires_internal_token(client: TestClient) -> None:
    response = client.post(
        "/internal/v1/documents/parse",
        data={
            "source_version_id": str(uuid4()),
            "source_format": "docx",
            "language": "zh-CN",
        },
        files={
            "file": (
                "sample.docx",
                build_docx(),
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            )
        },
    )

    assert response.status_code == 401


def test_parse_endpoint_returns_controlled_document(
    client: TestClient, internal_headers: dict[str, str]
) -> None:
    source_version_id = uuid4()
    response = client.post(
        "/internal/v1/documents/parse",
        headers=internal_headers,
        data={
            "source_version_id": str(source_version_id),
            "source_format": "docx",
            "language": "zh-CN",
        },
        files={
            "file": (
                "sample.docx",
                build_docx(),
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            )
        },
    )

    assert response.status_code == 200
    payload = response.json()
    assert payload["source_version_id"] == str(source_version_id)
    assert payload["document"]["model_version"] == "1.0"
    assert payload["document"]["metadata"]["title"] == "测试合同"
    assert any(node["kind"] == "table_cell" for node in payload["text_nodes"])


def test_parse_endpoint_rejects_signature_mismatch(
    client: TestClient, internal_headers: dict[str, str]
) -> None:
    response = client.post(
        "/internal/v1/documents/parse",
        headers=internal_headers,
        data={
            "source_version_id": str(uuid4()),
            "source_format": "pdf",
            "language": "en-US",
        },
        files={"file": ("fake.pdf", build_docx(), "application/pdf")},
    )

    assert response.status_code == 422
    assert response.json()["details"]["reason"] == "FILE_SIGNATURE_MISMATCH"
