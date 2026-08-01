#!/usr/bin/env python3
"""Exercise the real upload-to-template lifecycle against local infrastructure."""

from __future__ import annotations

import argparse
import copy
import hashlib
import json
import tempfile
import time
import urllib.error
import urllib.request
import uuid
from pathlib import Path
from typing import Any

from create_e2e_docx import build

DOCX_MIME = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"


class SmokeTestError(RuntimeError):
    """Raised when a smoke-test assertion or HTTP request fails."""


class ApiClient:
    def __init__(self, base_url: str) -> None:
        self.base_url = base_url.rstrip("/")
        self.token: str | None = None

    def json(
        self,
        method: str,
        path: str,
        body: dict[str, Any] | None = None,
        *,
        idempotent: bool = False,
        expected: tuple[int, ...] = (200,),
    ) -> Any:
        headers = {
            "Accept": "application/json",
            "X-Request-ID": str(uuid.uuid4()),
        }
        if self.token is not None:
            headers["Authorization"] = f"Bearer {self.token}"
        if body is not None:
            headers["Content-Type"] = "application/json"
        if idempotent:
            headers["Idempotency-Key"] = str(uuid.uuid4())
        request = urllib.request.Request(
            f"{self.base_url}{path}",
            data=None if body is None else json.dumps(body).encode("utf-8"),
            headers=headers,
            method=method,
        )
        try:
            with urllib.request.urlopen(request, timeout=30) as response:
                payload = response.read()
                if response.status not in expected:
                    raise SmokeTestError(
                        f"{method} {path}: expected {expected}, got {response.status}"
                    )
                return None if not payload else json.loads(payload)
        except urllib.error.HTTPError as error:
            details = error.read().decode("utf-8", errors="replace")
            raise SmokeTestError(
                f"{method} {path}: HTTP {error.code}: {details}"
            ) from error
        except urllib.error.URLError as error:
            raise SmokeTestError(f"{method} {path}: {error.reason}") from error

    def bytes(self, path: str) -> tuple[bytes, str]:
        headers = {"X-Request-ID": str(uuid.uuid4())}
        if self.token is not None:
            headers["Authorization"] = f"Bearer {self.token}"
        request = urllib.request.Request(f"{self.base_url}{path}", headers=headers)
        try:
            with urllib.request.urlopen(request, timeout=30) as response:
                return response.read(), response.headers.get_content_type()
        except urllib.error.HTTPError as error:
            details = error.read().decode("utf-8", errors="replace")
            raise SmokeTestError(f"GET {path}: HTTP {error.code}: {details}") from error


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SmokeTestError(message)


def upload(url: str, payload: bytes, required_headers: dict[str, str]) -> str:
    request = urllib.request.Request(
        url, data=payload, headers=required_headers, method="PUT"
    )
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            require(
                200 <= response.status < 300,
                f"object upload returned {response.status}",
            )
            return (response.headers.get("ETag") or "").replace('"', "")
    except urllib.error.HTTPError as error:
        details = error.read().decode("utf-8", errors="replace")
        raise SmokeTestError(f"object upload: HTTP {error.code}: {details}") from error


def edit_first_text(value: Any) -> bool:
    if isinstance(value, dict):
        if value.get("type") == "text" and isinstance(value.get("text"), str):
            value["text"] += "（联调已编辑）"
            return True
        return any(edit_first_text(child) for child in value.values())
    if isinstance(value, list):
        return any(edit_first_text(child) for child in value)
    return False


def wait_for_template(
    api: ApiClient, template_id: str, timeout: float
) -> dict[str, Any]:
    deadline = time.monotonic() + timeout
    last_status = "unknown"
    while time.monotonic() < deadline:
        detail = api.json("GET", f"/api/v1/templates/{template_id}")
        last_status = detail["template"]["conversion_status"]
        if last_status == "ready":
            return detail
        if last_status == "failed":
            failure = detail["template"].get("failure_code")
            raise SmokeTestError(f"template conversion failed: {failure}")
        time.sleep(0.5)
    raise SmokeTestError(f"template conversion timed out in status {last_status}")


def wait_for_extraction(
    api: ApiClient, extraction_id: str, timeout: float
) -> dict[str, Any]:
    deadline = time.monotonic() + timeout
    last_status = "unknown"
    while time.monotonic() < deadline:
        extraction = api.json("GET", f"/api/v1/extractions/{extraction_id}")
        last_status = extraction["status"]
        if last_status in ("review_required", "approved"):
            return extraction
        if last_status == "failed":
            failure = extraction.get("failure_code")
            raise SmokeTestError(f"extraction failed: {failure}")
        time.sleep(0.5)
    raise SmokeTestError(f"extraction timed out in status {last_status}")


def schema_field(
    key: str, json_path: str, description: str, position: int
) -> dict[str, Any]:
    return {
        "key": key,
        "json_path": json_path,
        "description": description,
        "value_type": "string",
        "array_item_type": None,
        "required": True,
        "nullable": False,
        "default": {"kind": "none"},
        "sensitivity": "high",
        "constraints": {
            "format": None,
            "pattern": None,
            "enum_values": [],
            "min_length": None,
            "max_length": None,
            "minimum": None,
            "maximum": None,
        },
        "examples": [],
        "extraction_hint": f"label:{description}",
        "display": {"mask": "partial", "view_role_keys": ["owner"]},
        "metadata": {"e2e": True},
        "position": position,
    }


def run_extraction_flow(
    api: ApiClient, workspace_id: str, source_version_id: str, timeout: float
) -> dict[str, Any]:
    schema = api.json(
        "POST",
        f"/api/v1/workspaces/{workspace_id}/schemas",
        {
            "name": f"STEP9 联系人字段 {uuid.uuid4().hex[:8]}",
            "description": "真实基础设施 E2E：敏感联系人字段",
            "fields": [
                schema_field("contact_name", "$.contact.name", "联系人姓名", 0),
                schema_field("contact_phone", "$.contact.phone", "联系人电话", 1),
                schema_field("contact_email", "$.contact.email", "联系人电子邮箱", 2),
            ],
        },
        idempotent=True,
        expected=(201,),
    )
    schema_version_id = schema["current_version"]["id"]

    sensitive_rules = api.json(
        "POST",
        f"/api/v1/workspaces/{workspace_id}/sensitive-rule-templates",
        {
            "name": f"STEP9 九国 PII {uuid.uuid4().hex[:8]}",
            "description": "真实基础设施 E2E：九国手机号补充规则",
            "rules": [
                {
                    "key": "international_phone",
                    "name": "International phone",
                    "description": "CN/US/JP/KR/DE/FR/GB/AU/NL phone recognizer",
                    "data_type": "phone_number",
                    "recognizer_kind": "regex",
                    "locales": ["zh-CN", "en-US"],
                    "country_codes": [
                        "CN",
                        "US",
                        "JP",
                        "KR",
                        "DE",
                        "FR",
                        "GB",
                        "AU",
                        "NL",
                    ],
                    "regex_pattern": r"(?:\+?86[\s-]?)?1[3-9](?:[\s-]?\d){9}",
                    "regex_dialect": "re2",
                    "dictionary_terms": [],
                    "validator_name": None,
                    "confidence_threshold": 0.85,
                    "priority": 100,
                    "enabled": True,
                }
            ],
        },
        idempotent=True,
        expected=(201,),
    )
    sensitive_version_id = sensitive_rules["current_version"]["id"]

    accepted = api.json(
        "POST",
        f"/api/v1/source-versions/{source_version_id}/extractions",
        {
            "schema_version_id": schema_version_id,
            "sensitive_rule_template_version_id": sensitive_version_id,
        },
        idempotent=True,
        expected=(202,),
    )
    extraction_id = accepted["extraction_id"]
    extraction = wait_for_extraction(api, extraction_id, timeout)
    require(extraction["status"] == "review_required", "extraction is not reviewable")
    result = extraction["result"]
    require(result is not None, "reviewable extraction has no result")
    require(result["model"]["provider"] == "mock", "unexpected model provider")
    expected_values = {
        "$.contact.name": "张伟",
        "$.contact.phone": "+86 138 0013 8000",
        "$.contact.email": "zhang.wei@example.cn",
    }
    fields = {field["json_path"]: field for field in result["fields"]}
    require(
        set(fields) == set(expected_values), "extraction fields do not match schema"
    )
    for json_path, expected in expected_values.items():
        field = fields[json_path]
        require(field["display_value"]["access"] == "visible", "owner value is masked")
        require(
            field["display_value"]["value"] == expected, f"wrong value at {json_path}"
        )
        require(bool(field["evidence"]), f"missing evidence at {json_path}")
        extraction = api.json(
            "PATCH",
            f"/api/v1/extractions/{extraction_id}/fields/{field['id']}",
            {"action": "accept", "value": None, "reason": None},
            idempotent=True,
        )

    approved = api.json(
        "POST",
        f"/api/v1/extractions/{extraction_id}/approve",
        {"note": "STEP9 真实基础设施 E2E 复核完成"},
        idempotent=True,
    )
    require(approved["status"] == "approved", "extraction was not approved")
    require(
        all(
            field["review_status"] == "accepted"
            for field in approved["result"]["fields"]
        ),
        "not all extraction fields were accepted",
    )
    return {
        "extraction_id": extraction_id,
        "schema_version_id": schema_version_id,
        "sensitive_rule_template_version_id": sensitive_version_id,
        "field_count": len(fields),
        "model_provider": result["model"]["provider"],
    }


def run(args: argparse.Namespace, fixture: Path) -> dict[str, Any]:
    api = ApiClient(args.api_base)
    login = api.json(
        "POST",
        "/api/v1/auth/login",
        {"email": args.email, "password": args.password},
    )
    api.token = login["access_token"]
    workspaces = api.json("GET", "/api/v1/workspaces")
    require(len(workspaces) > 0, "the authenticated user has no workspace")
    workspace = next(
        (item for item in workspaces if item.get("slug") == args.workspace_slug),
        workspaces[0],
    )

    file_bytes = fixture.read_bytes()
    sha256 = hashlib.sha256(file_bytes).hexdigest()
    created = api.json(
        "POST",
        f"/api/v1/workspaces/{workspace['id']}/sources",
        {
            "document_name": f"模板真实联调 {uuid.uuid4().hex[:8]}",
            "original_file_name": fixture.name,
            "declared_mime_type": DOCX_MIME,
            "size_bytes": len(file_bytes),
        },
        idempotent=True,
        expected=(201,),
    )
    upload_session = created["upload"]
    require(upload_session["upload_url"] is not None, "upload URL is missing")
    etag = upload(
        upload_session["upload_url"],
        file_bytes,
        upload_session["required_headers"],
    )
    require(bool(etag), "object storage did not return an ETag")

    version_id = created["version"]["id"]
    completed = api.json(
        "POST",
        f"/api/v1/source-versions/{version_id}/complete",
        {
            "size_bytes": len(file_bytes),
            "detected_mime_type": DOCX_MIME,
            "sha256": sha256,
            "object_etag": etag,
        },
        idempotent=True,
    )
    require(completed["version"]["status"] in ("uploaded", "ready"), "upload not ready")

    extraction = run_extraction_flow(api, workspace["id"], version_id, args.timeout)

    accepted = api.json(
        "POST",
        f"/api/v1/source-versions/{version_id}/templates",
        {"name": "跨境供应商尽调模板"},
        idempotent=True,
        expected=(202,),
    )
    template_id = accepted["template_id"]
    detail = wait_for_template(api, template_id, args.timeout)
    generated = detail["current_version"]
    require(generated is not None, "ready template has no current version")
    require(generated["version_number"] == 1, "generated version number is not 1")
    require(
        generated["document"]["html"].startswith('<article class="dm-document"'),
        "controlled HTML is missing",
    )
    require(
        generated["document"]["sanitization_policy_version"]
        == "docmind-controlled-html/v1",
        "unexpected HTML sanitization policy",
    )

    preview = api.json("GET", f"/api/v1/source-versions/{version_id}/preview")
    require(preview["view_url"] is not None, "derived PDF preview is not ready")
    require(
        preview["preview"]["page_count"] == 2, "derived preview page count is not 2"
    )
    original, original_type = api.bytes(preview["original_content_url"])
    require(
        original_type == DOCX_MIME, f"unexpected original MIME type: {original_type}"
    )
    require(
        hashlib.sha256(original).hexdigest() == sha256,
        "downloaded original digest mismatch",
    )
    preview_pdf, preview_type = api.bytes(preview["view_url"])
    require(
        preview_type == "application/pdf",
        f"unexpected preview MIME type: {preview_type}",
    )
    require(preview_pdf.startswith(b"%PDF"), "derived preview is not a PDF")

    edited_model = copy.deepcopy(generated["document_model"])
    require(edit_first_text(edited_model), "generated model has no editable text node")
    edited = api.json(
        "POST",
        f"/api/v1/templates/{template_id}/versions",
        {
            "base_version_id": generated["id"],
            "document_model": edited_model,
            "change_summary": "真实联调：修改首个文本节点",
        },
        idempotent=True,
    )
    require(edited["version_number"] == 2, "edited version number is not 2")
    require(
        edited["status"] == "checking", "edited version is not awaiting publication"
    )
    require(len(edited["diff"].get("changes", [])) > 0, "diff is empty")

    published = api.json(
        "POST",
        f"/api/v1/templates/{template_id}/versions/{edited['id']}/publish",
        {"note": "真实联调发布"},
        idempotent=True,
    )
    require(published["status"] == "published", "edited version was not published")

    rolled_back = api.json(
        "POST",
        f"/api/v1/templates/{template_id}/rollback",
        {
            "target_version_id": generated["id"],
            "change_summary": "真实联调：回滚至初始生成版本",
        },
        idempotent=True,
    )
    require(rolled_back["version_number"] == 3, "rollback version number is not 3")
    require(rolled_back["status"] == "published", "rollback version was not published")
    require(
        rolled_back["document_model"] == generated["document_model"],
        "rollback did not restore the target document model",
    )

    templates = api.json("GET", f"/api/v1/workspaces/{workspace['id']}/templates")
    require(
        any(item["id"] == template_id for item in templates),
        "template missing from list",
    )
    final_detail = api.json("GET", f"/api/v1/templates/{template_id}")
    require(
        final_detail["current_version"]["id"] == rolled_back["id"],
        "current version mismatch",
    )
    require(
        len(final_detail["versions"]) == 3,
        "template history does not contain 3 versions",
    )

    return {
        "workspace_id": workspace["id"],
        "source_id": created["source"]["id"],
        "source_version_id": version_id,
        "template_id": template_id,
        "conversion_job_id": accepted["job_id"],
        "version_count": len(final_detail["versions"]),
        "current_version": rolled_back["version_number"],
        "preview_bytes": len(preview_pdf),
        "warning_count": len(generated["warnings"]),
        **extraction,
    }


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--api-base", default="http://127.0.0.1:8080")
    parser.add_argument("--email", default="admin@docmind.local")
    parser.add_argument("--password", default="DocMind123!")
    parser.add_argument("--workspace-slug", default="docmind-demo")
    parser.add_argument("--timeout", type=float, default=90)
    parser.add_argument("--file", type=Path)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    if args.file is not None:
        result = run(args, args.file.resolve())
    else:
        with tempfile.TemporaryDirectory(prefix="docmind-e2e-") as directory:
            fixture = Path(directory) / "docmind-template-e2e.docx"
            build(fixture)
            result = run(args, fixture)
    print(json.dumps({"status": "passed", **result}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
