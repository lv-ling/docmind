from __future__ import annotations

from collections.abc import Iterable

from email_validator import EmailNotValidError, validate_email
from presidio_analyzer import Pattern, PatternRecognizer
from presidio_analyzer.predefined_recognizers import (
    CreditCardRecognizer,
    IbanRecognizer,
    IpRecognizer,
)

from docmind_ai.contracts.sensitive import SupportedCountryCode
from docmind_ai.pii.models import DetectionCandidate

PRESIDIO_ENTITY_TYPES = {
    "EMAIL_ADDRESS": "email_address",
    "IP_ADDRESS": "ip_address",
    "CREDIT_CARD": "credit_card",
    "IBAN_CODE": "bank_account",
}
IBAN_COUNTRIES: set[SupportedCountryCode] = {"DE", "FR", "GB", "NL"}


class PresidioDetector:
    """Run deterministic Presidio recognizers without loading an NER language model."""

    def __init__(self) -> None:
        self._recognizers = (
            PatternRecognizer(
                supported_entity="EMAIL_ADDRESS",
                name="DocMindInternationalEmailRecognizer",
                patterns=[
                    Pattern(
                        "international-email",
                        r"\b[!#$%&'*+\-/=?^_`{|}~\w](?:[!#$%&'*+\-/=?^_`{|}~.\w]{0,62}[!#$%&'*+\-/=?^_`{|}~\w])?@\w(?:[-\w]{0,61}\w)?(?:\.\w(?:[-\w]{0,61}\w)?)+\b",
                        0.8,
                    )
                ],
            ),
            IpRecognizer(),
            CreditCardRecognizer(),
            IbanRecognizer(),
        )

    def detect(
        self, text: str, *, country_codes: set[SupportedCountryCode]
    ) -> list[DetectionCandidate]:
        candidates: list[DetectionCandidate] = []
        for recognizer in self._recognizers:
            entities: list[str] = list(recognizer.supported_entities)
            results: Iterable[object] = recognizer.analyze(
                text=text,
                entities=entities,
                nlp_artifacts=None,
            )
            for result in results:
                entity_type = str(result.entity_type)  # type: ignore[attr-defined]
                start = int(result.start)  # type: ignore[attr-defined]
                end = int(result.end)  # type: ignore[attr-defined]
                score = float(result.score)  # type: ignore[attr-defined]
                raw_value = text[start:end]
                if entity_type == "EMAIL_ADDRESS" and not self._valid_email(raw_value):
                    continue
                country_code = self._iban_country(raw_value) if entity_type == "IBAN_CODE" else None
                if country_code is not None and country_code not in country_codes:
                    continue
                data_type = PRESIDIO_ENTITY_TYPES.get(entity_type)
                if data_type is None:
                    continue
                candidates.append(
                    DetectionCandidate(
                        start=start,
                        end=end,
                        data_type=data_type,  # type: ignore[arg-type]
                        score=max(score, 0.85 if entity_type == "EMAIL_ADDRESS" else score),
                        priority=self._priority(entity_type),
                        rule_key=f"presidio.{entity_type.lower()}",
                        normalized_value=self._normalized_value(entity_type, raw_value),
                        country_code=country_code,
                    )
                )
        return candidates

    @staticmethod
    def _valid_email(value: str) -> bool:
        try:
            validate_email(value, check_deliverability=False)
        except EmailNotValidError:
            return False
        return True

    @staticmethod
    def _iban_country(value: str) -> SupportedCountryCode | None:
        prefix = "".join(character for character in value.upper() if character.isalnum())[:2]
        return prefix if prefix in IBAN_COUNTRIES else None

    @staticmethod
    def _priority(entity_type: str) -> int:
        return {
            "EMAIL_ADDRESS": 500,
            "IP_ADDRESS": 300,
            "CREDIT_CARD": 650,
            "IBAN_CODE": 650,
        }.get(entity_type, 100)

    @staticmethod
    def _normalized_value(entity_type: str, value: str) -> str:
        if entity_type == "EMAIL_ADDRESS":
            try:
                return validate_email(value, check_deliverability=False).normalized.casefold()
            except EmailNotValidError:
                return value.casefold()
        return "".join(character for character in value.upper() if character.isalnum())
