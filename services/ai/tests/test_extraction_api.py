from fastapi.testclient import TestClient

from extraction_fixture import extraction_request, schema_field


def test_extraction_endpoint_runs_mock_workflow(
    client: TestClient, internal_headers: dict[str, str]
) -> None:
    request = extraction_request(
        fields=[schema_field(key="amount", description="合同金额", value_type="number")],
        texts=["合同金额：100万元"],
        json_schema={"type": "object", "properties": {"amount": {"type": "number"}}},
    )

    response = client.post(
        "/internal/v1/extractions/run",
        headers=internal_headers,
        json=request.model_dump(mode="json"),
    )

    assert response.status_code == 200
    payload = response.json()
    assert payload["result"]["data"] == {"amount": 100.0}
    assert payload["model"]["provider"] == "mock"
    assert payload["validation_errors"] == []


def test_extraction_endpoint_requires_internal_authentication(client: TestClient) -> None:
    request = extraction_request(
        fields=[schema_field(key="amount", value_type="number")],
        texts=["amount: 100"],
        json_schema={"type": "object", "properties": {"amount": {"type": "number"}}},
    )

    response = client.post(
        "/internal/v1/extractions/run",
        json=request.model_dump(mode="json"),
    )

    assert response.status_code == 401
