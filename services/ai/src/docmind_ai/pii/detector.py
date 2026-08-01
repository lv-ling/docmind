from __future__ import annotations

from collections.abc import Callable, Iterable
from dataclasses import dataclass, replace

import phonenumbers
import re2  # type: ignore[import-untyped]
from phonenumbers import Leniency, PhoneNumberFormat, PhoneNumberMatcher

from docmind_ai.contracts.sensitive import (
    SensitiveDataType,
    SensitiveRuleDefinition,
    SupportedCountryCode,
)
from docmind_ai.pii.models import DetectionCandidate
from docmind_ai.pii.normalization import compact_alphanumeric, normalize_width
from docmind_ai.pii.presidio_bridge import PresidioDetector
from docmind_ai.pii.validators import (
    valid_au_tfn,
    valid_cn_resident_identity,
    valid_de_tax_id,
    valid_fr_nir,
    valid_gb_nino,
    valid_jp_my_number,
    valid_kr_rrn,
    valid_nl_bsn,
    valid_us_ssn,
)

ALL_COUNTRIES: tuple[SupportedCountryCode, ...] = (
    "CN",
    "US",
    "JP",
    "KR",
    "DE",
    "FR",
    "GB",
    "AU",
    "NL",
)


class SensitiveConfigurationError(Exception):
    def __init__(self, rule_key: str, safe_message: str) -> None:
        super().__init__(safe_message)
        self.rule_key = rule_key
        self.safe_message = safe_message


@dataclass(frozen=True, slots=True)
class CountryIdentitySpec:
    country_code: SupportedCountryCode
    data_type: SensitiveDataType
    pattern: str
    validator: Callable[[str], bool]
    rule_key: str
    contexts: tuple[str, ...] = ()
    require_context: bool = False
    score: float = 0.97
    priority: int = 800


IDENTITY_SPECS = (
    CountryIdentitySpec(
        "CN",
        "china_national_id",
        r"(?:^|[^0-9])([0-9]{6}[0-9]{8}[0-9]{3}[0-9Xx]|[0-9]{15})(?:$|[^0-9A-Za-z])",
        valid_cn_resident_identity,
        "builtin.cn_resident_identity",
        priority=950,
    ),
    CountryIdentitySpec(
        "US",
        "identity_document",
        r"(?:^|[^0-9])([0-9]{3}[- ]?[0-9]{2}[- ]?[0-9]{4})(?:$|[^0-9])",
        valid_us_ssn,
        "builtin.us_ssn",
        contexts=("ssn", "social security", "社会安全"),
        priority=900,
    ),
    CountryIdentitySpec(
        "JP",
        "identity_document",
        r"(?:^|[^0-9])([0-9]{4}[- ]?[0-9]{4}[- ]?[0-9]{4})(?:$|[^0-9])",
        valid_jp_my_number,
        "builtin.jp_my_number",
        contexts=("マイナンバー", "個人番号", "my number"),
        require_context=True,
    ),
    CountryIdentitySpec(
        "KR",
        "identity_document",
        r"(?:^|[^0-9])([0-9]{6}[- ]?[1-8][0-9]{6})(?:$|[^0-9])",
        valid_kr_rrn,
        "builtin.kr_rrn",
        contexts=("주민등록번호", "외국인등록번호", "rrn"),
    ),
    CountryIdentitySpec(
        "DE",
        "identity_document",
        r"(?:^|[^0-9])([0-9](?:[ -]?[0-9]){10})(?:$|[^0-9])",
        valid_de_tax_id,
        "builtin.de_tax_id",
        contexts=("steueridentifikationsnummer", "steuer-id", "identifikationsnummer"),
        require_context=True,
    ),
    CountryIdentitySpec(
        "FR",
        "identity_document",
        (
            r"(?:^|[^0-9A-Za-z])([12][0-9]{2}[ -]?[0-9]{2}[ -]?"
            r"(?:[0-9]{2}|2A|2B)[ -]?[0-9]{3}[ -]?[0-9]{3}[ -]?"
            r"[0-9]{2})(?:$|[^0-9A-Za-z])"
        ),
        valid_fr_nir,
        "builtin.fr_nir",
        contexts=("numéro de sécurité sociale", "nir", "insee"),
        require_context=True,
    ),
    CountryIdentitySpec(
        "GB",
        "identity_document",
        r"(?:^|[^0-9A-Za-z])([A-Za-z]{2}[ -]?[0-9]{6}[ -]?[A-Da-d]?)(?:$|[^0-9A-Za-z])",
        valid_gb_nino,
        "builtin.gb_nino",
        contexts=("national insurance", "nino", "ni number"),
    ),
    CountryIdentitySpec(
        "AU",
        "identity_document",
        r"(?:^|[^0-9])([0-9]{3}[ -]?[0-9]{3}[ -]?[0-9]{2,3})(?:$|[^0-9])",
        valid_au_tfn,
        "builtin.au_tfn",
        contexts=("tax file number", "tfn"),
        require_context=True,
    ),
    CountryIdentitySpec(
        "NL",
        "identity_document",
        r"(?:^|[^0-9])([0-9]{8,9})(?:$|[^0-9])",
        valid_nl_bsn,
        "builtin.nl_bsn",
        contexts=("burgerservicenummer", "bsn"),
        require_context=True,
    ),
)

PASSPORT_PATTERNS: dict[SupportedCountryCode, str] = {
    "CN": r"(?:^|[^0-9A-Za-z])((?:EA[0-9]{7}|[EGPDS][0-9]{8}))(?:$|[^0-9A-Za-z])",
    "US": r"(?:^|[^0-9A-Za-z])([0-9A-Za-z]{9})(?:$|[^0-9A-Za-z])",
    "JP": r"(?:^|[^0-9A-Za-z])([A-Za-z]{2}[0-9]{7})(?:$|[^0-9A-Za-z])",
    "KR": r"(?:^|[^0-9A-Za-z])([A-Za-z][0-9]{8})(?:$|[^0-9A-Za-z])",
    "DE": r"(?:^|[^0-9A-Za-z])([CFGHJKLMNPRTVWXYZ0-9]{9})(?:$|[^0-9A-Za-z])",
    "FR": r"(?:^|[^0-9A-Za-z])([0-9]{2}[A-Za-z]{2}[0-9]{5})(?:$|[^0-9A-Za-z])",
    "GB": r"(?:^|[^0-9])([0-9]{9})(?:$|[^0-9])",
    "AU": r"(?:^|[^0-9A-Za-z])([A-Za-z]{1,2}[0-9]{7})(?:$|[^0-9A-Za-z])",
    "NL": r"(?:^|[^0-9A-Za-z])([A-Za-z0-9]{9})(?:$|[^0-9A-Za-z])",
}
PASSPORT_CONTEXT = (
    "passport",
    "护照",
    "旅券",
    "여권",
    "reisepass",
    "passeport",
    "paspoort",
)


class SensitiveDetector:
    def __init__(self) -> None:
        self._presidio = PresidioDetector()

    def detect(
        self,
        text: str,
        *,
        country_codes: Iterable[SupportedCountryCode] = ALL_COUNTRIES,
        rules: Iterable[SensitiveRuleDefinition] = (),
    ) -> list[DetectionCandidate]:
        normalized_text = normalize_width(text)
        allowed_countries = set(country_codes)
        candidates = self._presidio.detect(normalized_text, country_codes=allowed_countries)
        candidates.extend(self._phone_candidates(normalized_text, allowed_countries))
        candidates.extend(self._identity_candidates(normalized_text, allowed_countries))
        candidates.extend(self._passport_candidates(normalized_text, allowed_countries))
        candidates.extend(self._rule_candidates(normalized_text, rules, candidates))
        return self.resolve_overlaps(candidates)

    @staticmethod
    def resolve_overlaps(candidates: Iterable[DetectionCandidate]) -> list[DetectionCandidate]:
        ordered = sorted(
            candidates,
            key=lambda candidate: (
                -candidate.priority,
                -candidate.score,
                -(candidate.end - candidate.start),
                candidate.start,
                candidate.rule_key,
            ),
        )
        accepted: list[DetectionCandidate] = []
        seen: set[tuple[int, int, SensitiveDataType, str]] = set()
        for candidate in ordered:
            identity = (
                candidate.start,
                candidate.end,
                candidate.data_type,
                candidate.normalized_value,
            )
            if identity in seen or any(candidate.overlaps(existing) for existing in accepted):
                continue
            seen.add(identity)
            accepted.append(candidate)
        return sorted(accepted, key=lambda candidate: (candidate.start, candidate.end))

    @staticmethod
    def _phone_candidates(
        text: str, allowed_countries: set[SupportedCountryCode]
    ) -> list[DetectionCandidate]:
        candidates: list[DetectionCandidate] = []
        seen: set[tuple[int, int, str]] = set()
        for region in sorted(allowed_countries):
            for match in PhoneNumberMatcher(text, region, leniency=Leniency.VALID):
                number = match.number
                detected_region = phonenumbers.region_code_for_number(number)
                if detected_region not in allowed_countries or not phonenumbers.is_valid_number(
                    number
                ):
                    continue
                normalized_value = phonenumbers.format_number(number, PhoneNumberFormat.E164)
                identity = (match.start, match.end, normalized_value)
                if identity in seen:
                    continue
                seen.add(identity)
                candidates.append(
                    DetectionCandidate(
                        start=match.start,
                        end=match.end,
                        data_type="phone_number",
                        score=0.95,
                        priority=600,
                        rule_key="builtin.libphonenumber",
                        normalized_value=normalized_value,
                        country_code=detected_region,
                    )
                )
        return candidates

    @staticmethod
    def _identity_candidates(
        text: str, allowed_countries: set[SupportedCountryCode]
    ) -> list[DetectionCandidate]:
        candidates: list[DetectionCandidate] = []
        for spec in IDENTITY_SPECS:
            if spec.country_code not in allowed_countries:
                continue
            pattern = re2.compile(spec.pattern)
            for match in pattern.finditer(text):
                raw_value = match.group(1)
                start, end = match.span(1)
                if spec.require_context and not _has_context(text, start, end, spec.contexts):
                    continue
                if not spec.validator(raw_value):
                    continue
                candidates.append(
                    DetectionCandidate(
                        start=start,
                        end=end,
                        data_type=spec.data_type,
                        score=spec.score,
                        priority=spec.priority,
                        rule_key=spec.rule_key,
                        normalized_value=compact_alphanumeric(raw_value).upper(),
                        country_code=spec.country_code,
                    )
                )
        return candidates

    @staticmethod
    def _passport_candidates(
        text: str, allowed_countries: set[SupportedCountryCode]
    ) -> list[DetectionCandidate]:
        candidates: list[DetectionCandidate] = []
        for country_code in sorted(allowed_countries):
            pattern = re2.compile(
                PASSPORT_PATTERNS[country_code], options=_case_insensitive_options()
            )
            for match in pattern.finditer(text):
                start, end = match.span(1)
                if not _has_context(text, start, end, PASSPORT_CONTEXT):
                    continue
                raw_value = match.group(1)
                candidates.append(
                    DetectionCandidate(
                        start=start,
                        end=end,
                        data_type="passport",
                        score=0.9,
                        priority=850,
                        rule_key=f"builtin.{country_code.lower()}_passport",
                        normalized_value=compact_alphanumeric(raw_value).upper(),
                        country_code=country_code,
                    )
                )
        return candidates

    def _rule_candidates(
        self,
        text: str,
        rules: Iterable[SensitiveRuleDefinition],
        existing: list[DetectionCandidate],
    ) -> list[DetectionCandidate]:
        candidates: list[DetectionCandidate] = []
        for rule in rules:
            if not rule.enabled:
                continue
            if rule.recognizer_kind == "regex":
                candidates.extend(self._regex_rule_candidates(text, rule))
            elif rule.recognizer_kind == "dictionary":
                candidates.extend(self._dictionary_rule_candidates(text, rule))
            else:
                for candidate in existing:
                    if (
                        candidate.data_type == rule.data_type
                        and candidate.score >= rule.confidence_threshold
                    ):
                        candidates.append(
                            replace(
                                candidate,
                                score=max(candidate.score, rule.confidence_threshold),
                                priority=rule.priority,
                                rule_key=rule.key,
                            )
                        )
        return candidates

    @staticmethod
    def _regex_rule_candidates(
        text: str, rule: SensitiveRuleDefinition
    ) -> list[DetectionCandidate]:
        if not rule.regex_pattern or rule.regex_dialect != "re2":
            raise SensitiveConfigurationError(rule.key, "自定义正则规则缺少 RE2 表达式")
        try:
            pattern = re2.compile(rule.regex_pattern)
        except re2.error as exception:
            raise SensitiveConfigurationError(rule.key, "自定义正则规则无效") from exception
        return [
            DetectionCandidate(
                start=match.start(),
                end=match.end(),
                data_type=rule.data_type,
                score=rule.confidence_threshold,
                priority=rule.priority,
                rule_key=rule.key,
                normalized_value=match.group(0).casefold(),
            )
            for match in pattern.finditer(text)
            if match.end() > match.start()
        ]

    @staticmethod
    def _dictionary_rule_candidates(
        text: str, rule: SensitiveRuleDefinition
    ) -> list[DetectionCandidate]:
        if not rule.dictionary_terms:
            return []
        escaped_terms = [re2.escape(term) for term in rule.dictionary_terms if term]
        if sum(len(term) for term in escaped_terms) > 200_000:
            raise SensitiveConfigurationError(rule.key, "词典规则总长度超过扫描限制")
        try:
            pattern = re2.compile(
                "(?:" + "|".join(sorted(escaped_terms, key=len, reverse=True)) + ")",
                options=_case_insensitive_options(),
            )
        except re2.error as exception:
            raise SensitiveConfigurationError(rule.key, "词典规则无法编译") from exception
        return [
            DetectionCandidate(
                start=match.start(),
                end=match.end(),
                data_type=rule.data_type,
                score=rule.confidence_threshold,
                priority=rule.priority,
                rule_key=rule.key,
                normalized_value=match.group(0).casefold(),
            )
            for match in pattern.finditer(text)
        ]


def _has_context(text: str, start: int, end: int, terms: Iterable[str]) -> bool:
    window = text[max(0, start - 48) : min(len(text), end + 48)].casefold()
    return any(term.casefold() in window for term in terms)


def _case_insensitive_options() -> object:
    options = re2.Options()
    options.case_sensitive = False
    return options
