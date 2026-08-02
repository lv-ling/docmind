from __future__ import annotations

from dataclasses import dataclass

from docmind_ai.contracts.sensitive import SensitiveDataType, SupportedCountryCode


@dataclass(frozen=True, slots=True)
class DetectionCandidate:
    start: int
    end: int
    data_type: SensitiveDataType
    score: float
    priority: int
    rule_key: str
    normalized_value: str
    country_code: SupportedCountryCode | None = None

    def overlaps(self, other: DetectionCandidate) -> bool:
        return self.start < other.end and other.start < self.end


class SensitiveLeakError(Exception):
    def __init__(self, message: str, *, data_types: tuple[SensitiveDataType, ...] = ()) -> None:
        super().__init__(message)
        self.data_types = data_types
