from uuid import uuid4

import pytest
from pydantic import ValidationError

from docmind_ai.contracts.extraction import (
    AiExtractionRequest,
    ModelEvidence,
    ModelExtractionOutput,
    ModelFieldOutput,
    TokenizedDocument,
    TokenizedDocumentNode,
)
from docmind_ai.contracts.schema import (
    NoSchemaFieldDefault,
    SchemaFieldConstraints,
    SchemaFieldDefinition,
)


def test_extraction_contract_accepts_tokenized_document_only() -> None:
    source_version_id = uuid4()
    request = AiExtractionRequest(
        request_id=uuid4(),
        job_id=uuid4(),
        workspace_id=uuid4(),
        extraction_run_id=uuid4(),
        source_version_id=source_version_id,
        schema_version_id=uuid4(),
        sensitive_rule_template_version_id=None,
        fields=[
            SchemaFieldDefinition(
                id=uuid4(),
                key="amount",
                json_path="$.amount",
                description="合同金额",
                value_type="string",
                array_item_type=None,
                required=True,
                nullable=False,
                default=NoSchemaFieldDefault(kind="none"),
                sensitivity="none",
                constraints=SchemaFieldConstraints(),
                position=0,
            )
        ],
        json_schema={"type": "object"},
        sensitive_rules=[],
        document=TokenizedDocument(
            source_version_id=source_version_id,
            nodes=[
                TokenizedDocumentNode(
                    node_id="paragraph-1",
                    kind="paragraph",
                    page_number=1,
                    tokenized_text="联系人：[[SENSITIVE:PERSON:01]]",
                )
            ],
        ),
    )

    assert request.document.nodes[0].tokenized_text.endswith("]]")
    assert "original_value" not in request.model_dump_json()


def test_model_output_rejects_extra_fields_and_invalid_confidence() -> None:
    with pytest.raises(ValidationError):
        ModelExtractionOutput.model_validate(
            {
                "data": {"amount": "100"},
                "fields": [
                    {
                        "path": "$.amount",
                        "value": "100",
                        "confidence": 1.5,
                        "evidence": [],
                        "needs_review": False,
                        "unexpected": "not allowed",
                    }
                ],
            }
        )


def test_model_output_preserves_evidence_location() -> None:
    result = ModelExtractionOutput(
        data={"amount": "100"},
        fields=[
            ModelFieldOutput(
                path="$.amount",
                value="100",
                confidence=0.95,
                evidence=[
                    ModelEvidence(node_id="paragraph-2", page_number=2, tokenized_text="金额：100")
                ],
                needs_review=False,
            )
        ],
    )

    assert result.fields[0].evidence[0].page_number == 2
