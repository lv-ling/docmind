from io import BytesIO
from zipfile import BadZipFile, ZipFile

from docmind_ai.parsing.common import DocumentParsingError

MAX_ARCHIVE_ENTRIES = 10_000
MAX_UNCOMPRESSED_BYTES = 100 * 1024 * 1024
MAX_ENTRY_BYTES = 25 * 1024 * 1024
MAX_COMPRESSION_RATIO = 1_000


def validate_ooxml_archive(content: bytes) -> None:
    try:
        with ZipFile(BytesIO(content)) as archive:
            entries = archive.infolist()
            if len(entries) > MAX_ARCHIVE_ENTRIES:
                raise DocumentParsingError("DOCX_ARCHIVE_LIMIT", "DOCX 包含过多内部文件")

            total_size = 0
            names = {entry.filename for entry in entries}
            for entry in entries:
                if entry.filename.startswith("/") or ".." in entry.filename.split("/"):
                    raise DocumentParsingError("DOCX_UNSAFE_PATH", "DOCX 包含不安全的内部路径")
                if entry.file_size > MAX_ENTRY_BYTES:
                    raise DocumentParsingError("DOCX_ENTRY_LIMIT", "DOCX 内部文件超过安全限制")
                total_size += entry.file_size
                if total_size > MAX_UNCOMPRESSED_BYTES:
                    raise DocumentParsingError("DOCX_EXPANDED_LIMIT", "DOCX 解压后超过安全限制")
                if entry.compress_size == 0:
                    if entry.file_size > 0:
                        raise DocumentParsingError("DOCX_COMPRESSION_RATIO", "DOCX 压缩比异常")
                elif entry.file_size / entry.compress_size > MAX_COMPRESSION_RATIO:
                    raise DocumentParsingError("DOCX_COMPRESSION_RATIO", "DOCX 压缩比异常")

            required = {"[Content_Types].xml", "word/document.xml"}
            if not required.issubset(names):
                raise DocumentParsingError("DOCX_STRUCTURE_INVALID", "DOCX 缺少必要的 OOXML 结构")
    except BadZipFile as exception:
        raise DocumentParsingError("DOCX_ARCHIVE_INVALID", "DOCX 文件结构无效") from exception
