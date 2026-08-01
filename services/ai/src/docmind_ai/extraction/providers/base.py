from __future__ import annotations

from dataclasses import dataclass
from typing import Protocol

from docmind_ai.contracts.extraction import AiExtractionRequest, ModelExtractionOutput


@dataclass(frozen=True, slots=True)
class ProviderResult:
    output: ModelExtractionOutput
    provider: str
    model: str
    prompt_version: str
    input_tokens: int | None = None
    output_tokens: int | None = None


class ExtractionProvider(Protocol):
    async def extract(self, request: AiExtractionRequest) -> ProviderResult: ...


class ProviderTransientError(Exception):
    pass


class ProviderPermanentError(Exception):
    pass
