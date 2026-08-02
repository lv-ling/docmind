import json
from pathlib import Path

from jsonschema.validators import Draft202012Validator


def test_model_extraction_schema_is_valid_and_versioned() -> None:
    schema_path = (
        Path(__file__).resolve().parents[1]
        / "contracts"
        / "json-schema"
        / "model-extraction-output.schema.json"
    )
    schema = json.loads(schema_path.read_text(encoding="utf-8"))

    Draft202012Validator.check_schema(schema)
    assert schema["$schema"] == "https://json-schema.org/draft/2020-12/schema"
    assert schema["$id"] == "https://docmind.local/schemas/model-extraction-output.schema.json"
    assert schema["title"] == "ModelExtractionOutput"
