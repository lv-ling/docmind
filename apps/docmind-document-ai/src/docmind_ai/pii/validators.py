from __future__ import annotations

from datetime import datetime

from docmind_ai.pii.normalization import compact_alphanumeric, compact_digits

CN_PROVINCE_CODES = {
    "11",
    "12",
    "13",
    "14",
    "15",
    "21",
    "22",
    "23",
    "31",
    "32",
    "33",
    "34",
    "35",
    "36",
    "37",
    "41",
    "42",
    "43",
    "44",
    "45",
    "46",
    "50",
    "51",
    "52",
    "53",
    "54",
    "61",
    "62",
    "63",
    "64",
    "65",
    "71",
    "81",
    "82",
}


def valid_cn_resident_identity(value: str) -> bool:
    normalized = compact_alphanumeric(value).upper()
    if len(normalized) not in {15, 18} or not normalized[:17].isdigit():
        return False
    if normalized[:2] not in CN_PROVINCE_CODES:
        return False
    birth = normalized[6:14] if len(normalized) == 18 else f"19{normalized[6:12]}"
    if not _valid_date(birth, "%Y%m%d"):
        return False
    if len(normalized) == 15:
        return normalized.isdigit() and normalized[-3:] != "000"
    if normalized[-4:-1] == "000":
        return False
    weights = (7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2)
    check_characters = "10X98765432"
    check = check_characters[
        sum(int(digit) * weight for digit, weight in zip(normalized[:17], weights, strict=True))
        % 11
    ]
    return normalized[-1] == check


def valid_us_ssn(value: str) -> bool:
    digits = compact_digits(value)
    if len(digits) != 9:
        return False
    area = int(digits[:3])
    return area not in {0, 666} and area < 900 and digits[3:5] != "00" and digits[5:] != "0000"


def valid_jp_my_number(value: str) -> bool:
    digits = compact_digits(value)
    if len(digits) != 12:
        return False
    total = 0
    for position, digit in enumerate(reversed(digits[:11]), start=1):
        weight = position + 1 if position <= 6 else position - 5
        total += int(digit) * weight
    remainder = total % 11
    expected = 0 if remainder <= 1 else 11 - remainder
    return int(digits[-1]) == expected


def valid_kr_rrn(value: str) -> bool:
    digits = compact_digits(value)
    if len(digits) != 13:
        return False
    century_code = digits[6]
    century = "19" if century_code in "1256" else "20" if century_code in "3478" else None
    if century is None or not _valid_date(f"{century}{digits[:6]}", "%Y%m%d"):
        return False
    weights = (2, 3, 4, 5, 6, 7, 8, 9, 2, 3, 4, 5)
    expected = (
        11
        - sum(int(digit) * weight for digit, weight in zip(digits[:12], weights, strict=True)) % 11
    ) % 10
    return int(digits[-1]) == expected


def valid_de_tax_id(value: str) -> bool:
    digits = compact_digits(value)
    if len(digits) != 11 or digits[0] == "0":
        return False
    counts = sorted(digits[:10].count(str(number)) for number in range(10))
    if counts not in ([0, 1, 1, 1, 1, 1, 1, 1, 1, 2], [0, 0, 1, 1, 1, 1, 1, 1, 1, 3]):
        return False
    product = 10
    for digit in digits[:10]:
        checksum_sum = (int(digit) + product) % 10
        if checksum_sum == 0:
            checksum_sum = 10
        product = (2 * checksum_sum) % 11
    expected = 11 - product
    if expected == 10:
        expected = 0
    return expected == int(digits[-1])


def valid_fr_nir(value: str) -> bool:
    normalized = compact_alphanumeric(value).upper()
    if len(normalized) != 15:
        return False
    base = normalized[:13].replace("2A", "19").replace("2B", "18")
    key = normalized[13:]
    return base.isdigit() and key.isdigit() and 97 - (int(base) % 97) == int(key)


def valid_gb_nino(value: str) -> bool:
    normalized = compact_alphanumeric(value).upper()
    if len(normalized) not in {8, 9}:
        return False
    prefix = normalized[:2]
    if prefix in {"BG", "GB", "KN", "NK", "NT", "TN", "ZZ"}:
        return False
    if prefix[0] in "DFIQUV" or prefix[1] in "DFIOQUV":
        return False
    if not normalized[2:8].isdigit():
        return False
    return len(normalized) == 8 or normalized[-1] in "ABCD"


def valid_au_tfn(value: str) -> bool:
    digits = compact_digits(value)
    weights: tuple[int, ...]
    if len(digits) == 8:
        weights = (10, 7, 8, 4, 6, 3, 5, 1)
    elif len(digits) == 9:
        weights = (1, 4, 3, 7, 5, 8, 6, 9, 10)
    else:
        return False
    return sum(int(digit) * weight for digit, weight in zip(digits, weights, strict=True)) % 11 == 0


def valid_nl_bsn(value: str) -> bool:
    digits = compact_digits(value)
    if len(digits) == 8:
        digits = f"0{digits}"
    if len(digits) != 9 or digits == "000000000":
        return False
    weighted = sum(int(digits[index]) * (9 - index) for index in range(8)) - int(digits[8])
    return weighted % 11 == 0


def _valid_date(value: str, date_format: str) -> bool:
    try:
        parsed = datetime.strptime(value, date_format)
    except ValueError:
        return False
    return 1900 <= parsed.year <= datetime.now().year
