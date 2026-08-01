from __future__ import annotations

_WIDTH_TRANSLATION = {
    **{codepoint: codepoint - 0xFEE0 for codepoint in range(0xFF01, 0xFF5F)},
    0x3000: 0x20,
    0x00A0: 0x20,
    0x2007: 0x20,
    0x202F: 0x20,
    0x2010: ord("-"),
    0x2011: ord("-"),
    0x2012: ord("-"),
    0x2013: ord("-"),
    0x2212: ord("-"),
}


def normalize_width(text: str) -> str:
    """Normalize width and common separators without changing string offsets."""
    return text.translate(_WIDTH_TRANSLATION)


def compact_alphanumeric(text: str) -> str:
    return "".join(character for character in normalize_width(text) if character.isalnum())


def compact_digits(text: str) -> str:
    return "".join(character for character in normalize_width(text) if character.isdigit())
