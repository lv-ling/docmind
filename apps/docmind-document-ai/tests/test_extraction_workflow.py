from __future__ import annotations

import asyncio
from typing import Any

import pytest

from docmind_ai.contracts.extraction import (
    ModelEvidence,
    ModelExtractionOutput,
    ModelFieldOutput,
)
from docmind_ai.extraction import ExtractionWorkflow, LangChainChatProvider, MockExtractionProvider
from docmind_ai.extraction.providers import ProviderResult, ProviderTransientError
from docmind_ai.extraction.workflow_errors import ExtractionWorkflowError
from extraction_fixture import extraction_request, schema_field


def test_mock_workflow_extracts_defaults_null_and_evidence() -> None:
    fields = [
        schema_field(key="amount", description="合同金额", value_type="number", position=0),
        schema_field(
            key="note",
            description="备注",
            required=False,
            has_default=True,
            default="",
            position=1,
        ),
        schema_field(
            key="optional",
            description="可选项",
            required=False,
            nullable=True,
            position=2,
        ),
    ]
    request = extraction_request(
        fields=fields,
        texts=["合同金额：100万元"],
        json_schema={
            "type": "object",
            "additionalProperties": False,
            "required": ["amount", "note", "optional"],
            "properties": {
                "amount": {"type": "number"},
                "note": {"type": "string"},
                "optional": {"type": ["string", "null"]},
            },
        },
    )

    response = asyncio.run(ExtractionWorkflow(provider=MockExtractionProvider()).run(request))

    assert response.result.data == {"amount": 100.0, "note": "", "optional": None}
    assert response.result.fields[0].evidence[0].node_id == "node-0"
    assert response.result.fields[1].confidence is None
    assert response.result.fields[2].value is None
    assert response.validation_errors == []


def test_mock_workflow_preserves_multiple_candidates_for_review() -> None:
    field = schema_field(key="amount", description="合同金额", value_type="number")
    request = extraction_request(
        fields=[field],
        texts=["合同金额：100", "合同金额：200"],
        json_schema={"type": "object", "properties": {"amount": {"type": "number"}}},
    )

    response = asyncio.run(ExtractionWorkflow(provider=MockExtractionProvider()).run(request))
    result = response.result.fields[0]

    assert result.value == 100.0
    assert result.needs_review is True
    assert [candidate.value for candidate in result.candidates] == [100.0, 200.0]
    assert {candidate.evidence[0].page_number for candidate in result.candidates} == {1, 2}


def test_schema_validation_errors_are_safe_and_reviewable() -> None:
    request = extraction_request(
        fields=[schema_field(key="amount", value_type="string")],
        texts=["amount: not-a-number"],
        json_schema={"type": "object", "properties": {"amount": {"type": "number"}}},
    )

    response = asyncio.run(ExtractionWorkflow(provider=MockExtractionProvider()).run(request))

    assert response.validation_errors == ["$.amount: type validation failed"]
    assert "not-a-number" not in response.validation_errors[0]


@pytest.mark.parametrize(
    ("value", "expected_code"),
    [
        ("alice@example.com", "MODEL_PII_LEAK_DETECTED_EMAIL_ADDRESS"),
        ("[[SENSITIVE:EMAIL_ADDRESS:99]]", "MODEL_UNKNOWN_SENSITIVE_TOKEN"),
        ("[[SENSITIVE:EMAIL_ADDRESS:broken]]", "MODEL_MALFORMED_SENSITIVE_TOKEN"),
    ],
)
def test_model_output_security_gate_rejects_leaks_and_invalid_tokens(
    value: str, expected_code: str
) -> None:
    request = extraction_request(
        fields=[schema_field(key="contact")],
        texts=["contact: [[SENSITIVE:EMAIL_ADDRESS:01]]"],
        json_schema={"type": "object", "properties": {"contact": {"type": "string"}}},
    )
    provider = StaticProvider(_output(request, value=value))

    with pytest.raises(ExtractionWorkflowError) as error:
        asyncio.run(ExtractionWorkflow(provider=provider).run(request))

    assert error.value.code == expected_code


def test_evidence_must_align_to_known_node() -> None:
    request = extraction_request(
        fields=[schema_field(key="amount", value_type="number")],
        texts=["amount: 100"],
        json_schema={"type": "object", "properties": {"amount": {"type": "number"}}},
    )
    output = ModelExtractionOutput(
        data={"amount": 100},
        fields=[
            ModelFieldOutput(
                path="$.amount",
                value=100,
                confidence=0.9,
                evidence=[
                    ModelEvidence(
                        node_id="unknown-node", page_number=1, tokenized_text="amount: 100"
                    )
                ],
                candidates=[],
                needs_review=False,
            )
        ],
    )

    with pytest.raises(ExtractionWorkflowError) as error:
        asyncio.run(ExtractionWorkflow(provider=StaticProvider(output)).run(request))

    assert error.value.code == "MODEL_EVIDENCE_NODE_UNKNOWN"


def test_security_gate_does_not_scan_structural_node_identifiers_as_document_content() -> None:
    request = extraction_request(
        fields=[schema_field(key="status")],
        texts=["status: approved"],
        json_schema={"type": "object", "properties": {"status": {"type": "string"}}},
    )
    node = request.document.nodes[0].model_copy(update={"node_id": "node-123-45-6789"})
    request = request.model_copy(
        update={"document": request.document.model_copy(update={"nodes": [node]})}
    )

    response = asyncio.run(
        ExtractionWorkflow(provider=StaticProvider(_output(request, value="approved"))).run(request)
    )

    assert response.result.data == {"status": "approved"}


def test_transient_provider_is_retried_then_succeeds() -> None:
    request = extraction_request(
        fields=[schema_field(key="amount", value_type="number")],
        texts=["amount: 100"],
        json_schema={"type": "object", "properties": {"amount": {"type": "number"}}},
    )
    provider = FlakyProvider(_output(request, value=100), failures=2)

    response = asyncio.run(
        ExtractionWorkflow(
            provider=provider,
            retry_attempts=2,
            retry_backoff_seconds=0,
        ).run(request)
    )

    assert response.result.data == {"amount": 100}
    assert provider.calls == 3


def test_provider_timeout_and_token_budget_are_bounded() -> None:
    request = extraction_request(
        fields=[schema_field(key="amount", value_type="number")],
        texts=["amount: 100"],
        json_schema={"type": "object", "properties": {"amount": {"type": "number"}}},
    )

    with pytest.raises(ExtractionWorkflowError) as timeout_error:
        asyncio.run(
            ExtractionWorkflow(
                provider=SlowProvider(),
                provider_timeout_seconds=0.001,
                retry_attempts=0,
            ).run(request)
        )
    assert timeout_error.value.code == "MODEL_PROVIDER_UNAVAILABLE"
    assert timeout_error.value.retryable is True

    with pytest.raises(ExtractionWorkflowError) as budget_error:
        asyncio.run(
            ExtractionWorkflow(
                provider=MockExtractionProvider(),
                input_token_budget=100,
            ).run(request)
        )
    assert budget_error.value.code == "INPUT_TOKEN_BUDGET_EXCEEDED"


def test_langchain_adapter_sends_only_tokenized_safe_payload() -> None:
    request = extraction_request(
        fields=[schema_field(key="contact")],
        texts=["contact: [[SENSITIVE:EMAIL_ADDRESS:01]]"],
        json_schema={"type": "object", "properties": {"contact": {"type": "string"}}},
    )
    structured = CapturingStructuredModel(_output(request, value="[[SENSITIVE:EMAIL_ADDRESS:01]]"))
    model = FakeChatModel(structured)
    provider = LangChainChatProvider(
        chat_model=model,  # type: ignore[arg-type]
        provider_name="fake",
        model_name="fake-model",
        prompt_version="test-v1",
    )

    result = asyncio.run(provider.extract(request))

    payload = str(structured.messages)
    assert result.output.data["contact"] == "[[SENSITIVE:EMAIL_ADDRESS:01]]"
    assert "[[SENSITIVE:EMAIL_ADDRESS:01]]" in payload
    assert "sensitive_rules" not in payload


class StaticProvider:
    def __init__(self, output: ModelExtractionOutput) -> None:
        self.output = output

    async def extract(self, _request: Any) -> ProviderResult:
        return ProviderResult(
            output=self.output,
            provider="test",
            model="test",
            prompt_version="test-v1",
        )


class FlakyProvider(StaticProvider):
    def __init__(self, output: ModelExtractionOutput, *, failures: int) -> None:
        super().__init__(output)
        self.failures = failures
        self.calls = 0

    async def extract(self, request: Any) -> ProviderResult:
        self.calls += 1
        if self.calls <= self.failures:
            raise ProviderTransientError("temporary")
        return await super().extract(request)


class SlowProvider:
    async def extract(self, _request: Any) -> ProviderResult:
        await asyncio.sleep(1)
        raise AssertionError("timeout should cancel provider")


class CapturingStructuredModel:
    def __init__(self, output: ModelExtractionOutput) -> None:
        self.output = output
        self.messages: object | None = None

    async def ainvoke(self, messages: object) -> ModelExtractionOutput:
        self.messages = messages
        return self.output


class FakeChatModel:
    def __init__(self, structured: CapturingStructuredModel) -> None:
        self.structured = structured

    def with_structured_output(self, _schema: object) -> CapturingStructuredModel:
        return self.structured


def _output(request: Any, *, value: Any) -> ModelExtractionOutput:
    path = request.fields[0].json_path
    key = path.removeprefix("$.")
    return ModelExtractionOutput(
        data={key: value},
        fields=[
            ModelFieldOutput(
                path=path,
                value=value,
                confidence=0.9,
                evidence=[
                    ModelEvidence(
                        node_id=request.document.nodes[0].node_id,
                        page_number=request.document.nodes[0].page_number,
                        tokenized_text=request.document.nodes[0].tokenized_text,
                    )
                ],
                candidates=[],
                needs_review=False,
            )
        ],
    )
