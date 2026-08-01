#!/usr/bin/env python3
"""Verify DOC, DOCX and PDF source upload/preview across local services."""

from __future__ import annotations

import argparse
import hashlib
import json
import time
import urllib.error
import urllib.request
import uuid
from dataclasses import dataclass
from pathlib import Path
from typing import Any


class SmokeTestError(RuntimeError):
    """Raised when a smoke-test assertion or HTTP request fails."""


@dataclass(frozen=True)
class Fixture:
    path: Path
    mime_type: str
    file_type: str


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
        headers = {"Accept": "application/json", "X-Request-ID": str(uuid.uuid4())}
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
            require(200 <= response.status < 300, f"upload returned {response.status}")
            return (response.headers.get("ETag") or "").replace('"', "")
    except urllib.error.HTTPError as error:
        details = error.read().decode("utf-8", errors="replace")
        raise SmokeTestError(f"object upload: HTTP {error.code}: {details}") from error


def wait_for_preview(
    api: ApiClient, version_id: str, timeout: float
) -> dict[str, Any]:
    deadline = time.monotonic() + timeout
    last_status = "unknown"
    while time.monotonic() < deadline:
        preview = api.json("GET", f"/api/v1/source-versions/{version_id}/preview")
        last_status = preview["preview"]["status"]
        if last_status == "ready":
            return preview
        if last_status == "failed":
            failure = preview["preview"].get("failure_code")
            raise SmokeTestError(f"preview conversion failed: {failure}")
        time.sleep(0.5)
    raise SmokeTestError(f"preview conversion timed out in status {last_status}")


def upload_fixture(
    api: ApiClient,
    workspace_id: str,
    fixture: Fixture,
    timeout: float,
) -> dict[str, Any]:
    payload = fixture.path.read_bytes()
    sha256 = hashlib.sha256(payload).hexdigest()
    created = api.json(
        "POST",
        f"/api/v1/workspaces/{workspace_id}/sources",
        {
            "document_name": f"STEP9-{fixture.file_type.upper()}-{uuid.uuid4().hex[:6]}",
            "original_file_name": fixture.path.name,
            "declared_mime_type": fixture.mime_type,
            "size_bytes": len(payload),
        },
        idempotent=True,
        expected=(201,),
    )
    session = created["upload"]
    require(session["upload_url"] is not None, "upload URL is missing")
    etag = upload(session["upload_url"], payload, session["required_headers"])
    require(bool(etag), "object storage did not return an ETag")

    version_id = created["version"]["id"]
    api.json(
        "POST",
        f"/api/v1/source-versions/{version_id}/complete",
        {
            "size_bytes": len(payload),
            "detected_mime_type": fixture.mime_type,
            "sha256": sha256,
            "object_etag": etag,
        },
        idempotent=True,
    )
    preview = wait_for_preview(api, version_id, timeout)
    original, original_type = api.bytes(preview["original_content_url"])
    preview_pdf, preview_type = api.bytes(preview["view_url"])

    require(hashlib.sha256(original).hexdigest() == sha256, "original digest mismatch")
    require(original_type == fixture.mime_type, f"unexpected MIME: {original_type}")
    require(preview_type == "application/pdf", f"unexpected preview MIME: {preview_type}")
    require(preview_pdf.startswith(b"%PDF"), "preview payload is not a PDF")
    page_count = preview["preview"].get("page_count")
    require(
        isinstance(page_count, int) and page_count > 0,
        "preview page count must be a positive integer",
    )

    source = api.json("GET", f"/api/v1/sources/{created['source']['id']}")
    require(bool(source["versions"]), "source detail has no versions")
    current = max(source["versions"], key=lambda version: version["version_number"])
    require(current["status"] == "ready", "source version was not marked ready")
    require(current["file_type"] == fixture.file_type, "detected file type mismatch")

    return {
        "file_type": fixture.file_type,
        "source_id": created["source"]["id"],
        "source_version_id": version_id,
        "original_bytes": len(original),
        "preview_bytes": len(preview_pdf),
        "page_count": page_count,
    }


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--api-base", default="http://127.0.0.1:8080")
    parser.add_argument("--email", default="admin@docmind.local")
    parser.add_argument("--password", default="DocMind123!")
    parser.add_argument("--workspace-slug", default="docmind-demo")
    parser.add_argument("--timeout", type=float, default=90)
    parser.add_argument("--doc", type=Path, required=True)
    parser.add_argument("--docx", type=Path, required=True)
    parser.add_argument("--pdf", type=Path, required=True)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    api = ApiClient(args.api_base)
    login = api.json(
        "POST",
        "/api/v1/auth/login",
        {"email": args.email, "password": args.password},
    )
    api.token = login["access_token"]
    workspaces = api.json("GET", "/api/v1/workspaces")
    require(bool(workspaces), "the authenticated user has no workspace")
    workspace = next(
        (item for item in workspaces if item.get("slug") == args.workspace_slug),
        workspaces[0],
    )
    fixtures = (
        Fixture(args.doc.resolve(), "application/msword", "doc"),
        Fixture(
            args.docx.resolve(),
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "docx",
        ),
        Fixture(args.pdf.resolve(), "application/pdf", "pdf"),
    )
    for fixture in fixtures:
        require(fixture.path.is_file(), f"fixture does not exist: {fixture.path}")

    results = [
        upload_fixture(api, workspace["id"], fixture, args.timeout)
        for fixture in fixtures
    ]
    print(
        json.dumps(
            {"status": "passed", "workspace_id": workspace["id"], "formats": results},
            ensure_ascii=False,
            indent=2,
        )
    )


if __name__ == "__main__":
    main()
