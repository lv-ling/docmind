from __future__ import annotations

import asyncio
import json
from pathlib import Path
from typing import Any
from uuid import UUID

from pydantic import Field, JsonValue

from docmind_ai.contracts.common import StrictModel
from docmind_ai.contracts.extraction import (
    AiExtractionRequest,
    TokenizedDocument,
    TokenizedDocumentNode,
)
from docmind_ai.contracts.schema import (
    LiteralSchemaFieldDefault,
    NoSchemaFieldDefault,
    SchemaFieldConstraints,
    SchemaFieldDefinition,
)
from docmind_ai.extraction import ExtractionWorkflow, MockExtractionProvider

FIXTURE_PATH = Path(__file__).parents[3] / "evals" / "mock_extraction_cases.json"
FIXTURE_NAMESPACE = UUID("73cd50c4-9336-4912-b023-b0177434821f")


class EvaluationField(StrictModel):
    key: str
    description: str
    value_type: str
    required: bool = True
    nullable: bool = False
    has_default: bool = False
    default: JsonValue = None


class EvaluationCase(StrictModel):
    id: str
    fields: list[EvaluationField]
    nodes: list[str]
    json_schema: dict[str, JsonValue]
    expected_data: dict[str, JsonValue]
    expected_review_paths: list[str] = Field(default_factory=list)


class EvaluationSummary(StrictModel):
    total_cases: int
    passed_cases: int
    data_accuracy: float = Field(ge=0, le=1)
    review_accuracy: float = Field(ge=0, le=1)
    schema_pass_rate: float = Field(ge=0, le=1)
    failed_case_ids: list[str]


async def run_evaluation(fixture_path: Path = FIXTURE_PATH) -> EvaluationSummary:
    fixture_text = await asyncio.to_thread(fixture_path.read_text, encoding="utf-8")
    raw_cases: Any = json.loads(fixture_text)
    cases = [EvaluationCase.model_validate(case) for case in raw_cases]
    workflow = ExtractionWorkflow(provider=MockExtractionProvider())
    data_passes = 0
    review_passes = 0
    schema_passes = 0
    failed_case_ids: list[str] = []
    for case_index, case in enumerate(cases):
        request = _request_for_case(case, case_index)
        response = await workflow.run(request)
        data_matches = response.result.data == case.expected_data
        actual_review_paths = sorted(
            field.path for field in response.result.fields if field.needs_review
        )
        review_matches = actual_review_paths == sorted(case.expected_review_paths)
        schema_matches = not response.validation_errors
        data_passes += int(data_matches)
        review_passes += int(review_matches)
        schema_passes += int(schema_matches)
        if not (data_matches and review_matches and schema_matches):
            failed_case_ids.append(case.id)
    total = len(cases)
    denominator = max(total, 1)
    return EvaluationSummary(
        total_cases=total,
        passed_cases=total - len(failed_case_ids),
        data_accuracy=data_passes / denominator,
        review_accuracy=review_passes / denominator,
        schema_pass_rate=schema_passes / denominator,
        failed_case_ids=failed_case_ids,
    )


def main() -> None:
    summary = asyncio.run(run_evaluation())
    print(summary.model_dump_json())
    if summary.failed_case_ids:
        raise SystemExit(1)


def _request_for_case(case: EvaluationCase, case_index: int) -> AiExtractionRequest:
    fields = [
        SchemaFieldDefinition(
            id=UUID(int=1000 + case_index * 100 + field_index),
            key=field.key,
            json_path=f"$.{field.key}",
            description=field.description,
            value_type=field.value_type,  # type: ignore[arg-type]
            array_item_type=None,
            required=field.required,
            nullable=field.nullable,
            default=(
                LiteralSchemaFieldDefault(kind="literal", value=field.default)
                if field.has_default
                else NoSchemaFieldDefault(kind="none")
            ),
            sensitivity="none",
            constraints=SchemaFieldConstraints(),
            examples=[],
            extraction_hint=None,
            position=field_index,
        )
        for field_index, field in enumerate(case.fields)
    ]
    source_version_id = UUID(int=2000 + case_index)
    return AiExtractionRequest(
        request_id=UUID(int=3000 + case_index),
        job_id=UUID(int=4000 + case_index),
        workspace_id=UUID(int=5000 + case_index),
        extraction_run_id=UUID(int=6000 + case_index),
        source_version_id=source_version_id,
        schema_version_id=UUID(int=7000 + case_index),
        sensitive_rule_template_version_id=None,
        fields=fields,
        json_schema=case.json_schema,
        sensitive_rules=[],
        document=TokenizedDocument(
            source_version_id=source_version_id,
            language="zh-CN",
            nodes=[
                TokenizedDocumentNode(
                    node_id=f"case-{case.id}-node-{node_index}",
                    kind="paragraph",
                    page_number=node_index + 1,
                    tokenized_text=text,
                    metadata={},
                )
                for node_index, text in enumerate(case.nodes)
            ],
        ),
    )


if __name__ == "__main__":
    main()
