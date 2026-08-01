from __future__ import annotations

from uuid import UUID

from docmind_ai.contracts.document import ConversionWarning, DocumentFormat, ParseDocumentResponse
from docmind_ai.parsing.docx_parser import DocxParser
from docmind_ai.parsing.libreoffice import LibreOfficeConverter
from docmind_ai.parsing.pdf_parser import PdfParser


class DocumentParsingService:
    def __init__(
        self,
        *,
        libreoffice_binary: str = "libreoffice",
        conversion_timeout_seconds: float = 30,
        max_output_bytes: int = 25 * 1024 * 1024,
        max_pdf_pages: int = 500,
    ) -> None:
        self._docx_parser = DocxParser()
        self._pdf_parser = PdfParser(max_pages=max_pdf_pages)
        self._converter = LibreOfficeConverter(
            binary=libreoffice_binary,
            timeout_seconds=conversion_timeout_seconds,
            max_output_bytes=max_output_bytes,
        )

    def parse(
        self,
        *,
        source_version_id: UUID,
        source_format: DocumentFormat,
        content: bytes,
        language: str,
    ) -> ParseDocumentResponse:
        if source_format == "pdf":
            return self._pdf_parser.parse(
                source_version_id=source_version_id,
                content=content,
                language=language,
                source_format=source_format,
            )

        converted = source_format == "doc"
        docx_content = self._converter.convert_doc_to_docx(content) if converted else content
        response = self._docx_parser.parse(
            source_version_id=source_version_id,
            content=docx_content,
            language=language,
            source_format=source_format,
        )
        if converted:
            warning = response.warnings.copy()
            warning.append(
                ConversionWarning(
                    code="DOC_CONVERTED_TO_DOCX",
                    severity="info",
                    message="旧版 DOC 已通过隔离的 LibreOffice 流程转换为 DOCX 后解析",
                )
            )
            return response.model_copy(update={"warnings": warning})
        return response
