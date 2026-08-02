from __future__ import annotations

import shutil
import subprocess
import tempfile
from pathlib import Path

from docmind_ai.parsing.common import DocumentParsingError


class LibreOfficeConverter:
    def __init__(self, *, binary: str, timeout_seconds: float, max_output_bytes: int) -> None:
        self._binary = binary
        self._timeout_seconds = timeout_seconds
        self._max_output_bytes = max_output_bytes

    def is_available(self) -> bool:
        return self._resolve_binary() is not None

    def convert_doc_to_docx(self, content: bytes) -> bytes:
        binary = self._resolve_binary()
        if binary is None:
            raise DocumentParsingError("LIBREOFFICE_UNAVAILABLE", "当前环境未配置 DOC 转换组件")

        with tempfile.TemporaryDirectory(prefix="docmind-convert-") as temporary_directory:
            workdir = Path(temporary_directory)
            input_path = workdir / "source.doc"
            output_path = workdir / "source.docx"
            input_path.write_bytes(content)
            command = [
                binary,
                "--headless",
                "--nologo",
                "--nodefault",
                "--nofirststartwizard",
                "--nolockcheck",
                "--convert-to",
                "docx:Office Open XML Text",
                "--outdir",
                str(workdir),
                str(input_path),
            ]
            try:
                completed = subprocess.run(  # noqa: S603
                    command,
                    stdin=subprocess.DEVNULL,
                    capture_output=True,
                    timeout=self._timeout_seconds,
                    check=False,
                    env={"HOME": str(workdir), "LANG": "C.UTF-8", "PATH": "/usr/bin:/bin"},
                )
            except subprocess.TimeoutExpired as exception:
                raise DocumentParsingError("DOC_CONVERSION_TIMEOUT", "DOC 转换超时") from exception
            if completed.returncode != 0 or not output_path.is_file():
                raise DocumentParsingError("DOC_CONVERSION_FAILED", "DOC 转换失败")
            if output_path.stat().st_size > self._max_output_bytes:
                raise DocumentParsingError(
                    "DOC_CONVERSION_OUTPUT_LIMIT", "DOC 转换结果超过安全限制"
                )
            return output_path.read_bytes()

    def _resolve_binary(self) -> str | None:
        candidate = shutil.which(self._binary)
        if candidate is None:
            return None
        resolved = Path(candidate).resolve()
        return str(resolved) if resolved.is_file() else None
