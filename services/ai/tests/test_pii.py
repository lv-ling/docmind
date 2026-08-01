from __future__ import annotations

from uuid import uuid4

import pytest

from docmind_ai.contracts.sensitive import (
    SensitiveRuleDefinition,
    SensitiveTextNode,
    SensitiveTokenizationRequest,
)
from docmind_ai.pii import SensitiveDetector, SensitiveTokenizer
from docmind_ai.pii.detector import SensitiveConfigurationError
from docmind_ai.pii.models import SensitiveLeakError
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

ALL_COUNTRIES = ["CN", "US", "JP", "KR", "DE", "FR", "GB", "AU", "NL"]


def test_phone_detector_covers_configured_countries() -> None:
    text = (
        "CN +8613800138000 US +12025550123 JP +81312345678 "
        "KR +8221234567 DE +4930123456 FR +33142345678 "
        "GB +442083661177 AU +61293744000 NL +31201234567"
    )

    detections = SensitiveDetector().detect(text)

    detected_countries = {
        detection.country_code for detection in detections if detection.data_type == "phone_number"
    }
    assert detected_countries == set(ALL_COUNTRIES)


def test_international_email_presidio_card_iban_and_ip_detection() -> None:
    text = "邮箱 用户@例子.公司 IP 192.168.1.1 卡 4111 1111 1111 1111 IBAN DE89370400440532013000"

    detections = SensitiveDetector().detect(text)

    assert {detection.data_type for detection in detections} == {
        "email_address",
        "ip_address",
        "credit_card",
        "bank_account",
    }


def test_country_identity_validators_reject_bad_checksums() -> None:
    french_base = "1800675012345"
    french_nir = f"{french_base}{97 - (int(french_base) % 97):02d}"
    japanese_base = "12345678901"
    weighted = sum(
        int(digit) * (position + 1 if position <= 6 else position - 5)
        for position, digit in enumerate(reversed(japanese_base), start=1)
    )
    remainder = weighted % 11
    japanese_number = f"{japanese_base}{0 if remainder <= 1 else 11 - remainder}"
    korean_base = "900101123456"
    korean_weights = (2, 3, 4, 5, 6, 7, 8, 9, 2, 3, 4, 5)
    korean_check = (
        11
        - sum(
            int(digit) * weight for digit, weight in zip(korean_base, korean_weights, strict=True)
        )
        % 11
    ) % 10
    korean_number = f"{korean_base}{korean_check}"

    checksum_samples = [
        (valid_cn_resident_identity, "11010519491231002X"),
        (valid_jp_my_number, japanese_number),
        (valid_kr_rrn, korean_number),
        (valid_de_tax_id, "86095742719"),
        (valid_fr_nir, french_nir),
        (valid_au_tfn, "123456782"),
        (valid_nl_bsn, "111222333"),
    ]
    for validator, sample in checksum_samples:
        assert validator(sample), sample
        invalid_last_digit = f"{sample[:-1]}{'0' if sample[-1] != '0' else '1'}"
        assert not validator(invalid_last_digit), sample
    assert valid_us_ssn("123-45-6789")
    assert not valid_us_ssn("000-45-6789")
    assert valid_gb_nino("AB123456C")
    assert not valid_gb_nino("BG123456C")


def test_custom_re2_and_dictionary_rules_are_supported() -> None:
    rules = [
        _rule(
            key="tenant_account",
            recognizer_kind="regex",
            regex_pattern=r"ACCT-[0-9]{4}",
            dictionary_terms=[],
        ),
        _rule(
            key="tenant_secret_word",
            recognizer_kind="dictionary",
            regex_pattern=None,
            dictionary_terms=["蓝鲸计划"],
        ),
    ]

    detections = SensitiveDetector().detect("编号 ACCT-1234, 项目 蓝鲸计划", rules=rules)

    assert [detection.rule_key for detection in detections] == [
        "tenant_account",
        "tenant_secret_word",
    ]


def test_invalid_re2_rule_is_rejected() -> None:
    rule = _rule(
        key="unsafe",
        recognizer_kind="regex",
        regex_pattern=r"(?=secret)",
        dictionary_terms=[],
    )

    with pytest.raises(SensitiveConfigurationError, match="无效"):
        SensitiveDetector().detect("secret", rules=[rule])


def test_tokenization_is_stable_deduplicated_and_contains_no_original_mapping() -> None:
    source_version_id = uuid4()
    request = SensitiveTokenizationRequest(
        source_version_id=source_version_id,
        language="zh-CN",
        country_codes=ALL_COUNTRIES,
        rules=[],
        nodes=[
            SensitiveTextNode(
                node_id="paragraph-1",
                kind="paragraph",
                page_number=1,
                text="身份证 11010519491231002X, 邮箱 alice@example.com",
            ),
            SensitiveTextNode(
                node_id="paragraph-2",
                kind="paragraph",
                page_number=2,
                text="再次联系 alice@example.com 或 +86 13800138000",
            ),
        ],
    )

    first = SensitiveTokenizer().tokenize(request)
    second = SensitiveTokenizer().tokenize(request)

    assert first == second
    email_token = next(token for token in first.tokens if token.data_type == "email_address")
    assert len(email_token.occurrences) == 2
    assert all("alice@example.com" not in node.tokenized_text for node in first.nodes)
    serialized = first.model_dump_json()
    assert "11010519491231002X" not in serialized
    assert "alice@example.com" not in serialized
    assert "original_value" not in serialized


def test_secondary_leak_scan_rejects_plaintext() -> None:
    tokenizer = SensitiveTokenizer()

    with pytest.raises(SensitiveLeakError, match="敏感明文"):
        tokenizer.assert_no_plaintext_pii(
            "模型意外返回 alice@example.com",
            country_codes=ALL_COUNTRIES,
        )


def _rule(
    *,
    key: str,
    recognizer_kind: str,
    regex_pattern: str | None,
    dictionary_terms: list[str],
) -> SensitiveRuleDefinition:
    return SensitiveRuleDefinition(
        id=uuid4(),
        key=key,
        name=key,
        description="测试规则",
        data_type="custom",
        recognizer_kind=recognizer_kind,
        locales=[],
        country_codes=[],
        regex_pattern=regex_pattern,
        regex_dialect="re2" if regex_pattern else None,
        dictionary_terms=dictionary_terms,
        validator_name=None,
        confidence_threshold=0.9,
        priority=1000,
        enabled=True,
    )
