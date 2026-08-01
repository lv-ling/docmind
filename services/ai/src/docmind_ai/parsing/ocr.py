from __future__ import annotations

from typing import Protocol

from pydantic import Field

from docmind_ai.contracts.common import StrictModel


class OcrTextBlock(StrictModel):
    text: str = Field(min_length=1, max_length=100_000)
    left: float
    top: float
    right: float
    bottom: float
    confidence: float | None = Field(default=None, ge=0, le=1)


class OcrAdapter(Protocol):
    @property
    def available(self) -> bool: ...

    def extract_page(self, *, pdf_content: bytes, page_number: int) -> list[OcrTextBlock]: ...


class DisabledOcrAdapter:
    @property
    def available(self) -> bool:
        return False

    def extract_page(self, *, pdf_content: bytes, page_number: int) -> list[OcrTextBlock]:
        del pdf_content, page_number
        return []
