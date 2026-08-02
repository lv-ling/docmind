from __future__ import annotations

import re
import statistics
from dataclasses import dataclass
from io import BytesIO
from typing import Any, Literal, cast
from uuid import UUID

import pdfplumber
from pydantic import JsonValue

from docmind_ai.contracts.document import (
    BlockNode,
    ControlledDocument,
    ControlledDocumentMetadata,
    HeaderFooterRegion,
    HeadingNode,
    ImageNode,
    PageMarkerNode,
    ParagraphNode,
    ParagraphStyle,
    ParseDocumentResponse,
    ParsedTextNode,
    SourceNodeReference,
    TableCellNode,
    TableCellStyle,
    TableNode,
    TableRowNode,
    TextNode,
    TextStyle,
)
from docmind_ai.parsing.common import (
    DocumentParsingError,
    ParseContext,
    default_page_layout,
    points,
)
from docmind_ai.parsing.ocr import DisabledOcrAdapter, OcrAdapter

PARSER_VERSION = "pdf-layout/1.0"


@dataclass(slots=True)
class PdfPageData:
    page_number: int
    width: float
    height: float
    lines: list[dict[str, Any]]
    tables: list[Any]
    images: list[dict[str, Any]]


class PdfParser:
    def __init__(self, *, max_pages: int, ocr_adapter: OcrAdapter | None = None) -> None:
        self._max_pages = max_pages
        self._ocr_adapter = ocr_adapter or DisabledOcrAdapter()

    def parse(
        self,
        *,
        source_version_id: UUID,
        content: bytes,
        language: str,
        source_format: Literal["pdf"],
    ) -> ParseDocumentResponse:
        context = ParseContext(source_version_id=source_version_id, language=language)
        try:
            with pdfplumber.open(BytesIO(content), unicode_norm="NFC") as pdf:
                if len(pdf.pages) > self._max_pages:
                    raise DocumentParsingError(
                        "PDF_PAGE_LIMIT", f"PDF 页数超过 {self._max_pages} 页安全限制"
                    )
                page_data = [self._read_page(page) for page in pdf.pages]
                title = str(pdf.metadata.get("Title") or "") if pdf.metadata else ""
        except DocumentParsingError:
            raise
        except Exception as exception:
            raise DocumentParsingError(
                "PDF_STRUCTURE_INVALID", "PDF 文件结构无效或已加密"
            ) from exception

        header_keys, footer_keys = self._repeated_margin_keys(page_data)
        blocks: list[BlockNode] = []
        header_blocks: list[BlockNode] = []
        footer_blocks: list[BlockNode] = []
        body_font_sizes = self._body_font_sizes(page_data, header_keys | footer_keys)
        median_body_size = statistics.median(body_font_sizes) if body_font_sizes else 10.0

        for page in page_data:
            blocks.append(
                PageMarkerNode(
                    id=context.node_id(f"pdf/page/{page.page_number}/marker"),
                    source=SourceNodeReference(
                        source_node_id=f"pdf/page/{page.page_number}",
                        page_number=page.page_number,
                    ),
                    attributes={},
                    page_number=page.page_number,
                )
            )
            table_bboxes = [self._bbox_tuple(table.bbox) for table in page.tables]
            elements: list[tuple[float, int, Any]] = []
            for line_index, line in enumerate(page.lines):
                if any(self._inside_bbox(line, bbox) for bbox in table_bboxes):
                    continue
                elements.append((float(line.get("top", 0)), 0, (line_index, line)))
            for table_index, table in enumerate(page.tables):
                elements.append((float(table.bbox[1]), 1, (table_index, table)))
            for image_index, image in enumerate(page.images):
                elements.append((float(image.get("top", 0)), 2, (image_index, image)))

            page_has_text = False
            for _top, element_type, payload in sorted(
                elements, key=lambda item: (item[0], item[1])
            ):
                if element_type == 0:
                    line_index, line = payload
                    text = str(line.get("text") or "").strip()
                    if not text:
                        continue
                    page_has_text = True
                    key = self._line_key(line, page)
                    region_kind: Literal["header", "footer", "paragraph"] = "paragraph"
                    if key in header_keys:
                        region_kind = "header"
                    elif key in footer_keys:
                        region_kind = "footer"
                    node = self._line_node(
                        line,
                        context=context,
                        locator=f"pdf/page/{page.page_number}/line/{line_index}",
                        page_number=page.page_number,
                        median_body_size=median_body_size,
                        region_kind=region_kind,
                    )
                    if region_kind == "header":
                        if not header_blocks:
                            header_blocks.append(node)
                    elif region_kind == "footer":
                        if not footer_blocks:
                            footer_blocks.append(node)
                    else:
                        blocks.append(node)
                elif element_type == 1:
                    table_index, table = payload
                    page_has_text = True
                    blocks.append(
                        self._table_node(
                            table,
                            context=context,
                            locator=f"pdf/page/{page.page_number}/table/{table_index}",
                            page_number=page.page_number,
                        )
                    )
                else:
                    image_index, image = payload
                    blocks.append(
                        self._image_node(
                            image,
                            context=context,
                            locator=f"pdf/page/{page.page_number}/image/{image_index}",
                            page_number=page.page_number,
                        )
                    )

            if not page_has_text:
                self._handle_ocr_page(
                    content,
                    context=context,
                    page=page,
                    blocks=blocks,
                    median_body_size=median_body_size,
                )

        if not page_data:
            raise DocumentParsingError("PDF_EMPTY", "PDF 不包含可解析页面")
        first_page = page_data[0]
        controlled_document = ControlledDocument(
            root_id=context.node_id("root"),
            template_schema_version_id=None,
            metadata=ControlledDocumentMetadata(
                title=title,
                language=language,
                source_page_count=len(page_data),
            ),
            page_layout=default_page_layout(width=first_page.width, height=first_page.height),
            headers=(
                [HeaderFooterRegion(variant="default", blocks=header_blocks)]
                if header_blocks
                else []
            ),
            footers=(
                [HeaderFooterRegion(variant="default", blocks=footer_blocks)]
                if footer_blocks
                else []
            ),
            blocks=blocks,
        )
        return ParseDocumentResponse(
            source_version_id=source_version_id,
            source_format=source_format,
            parser_version=PARSER_VERSION,
            document=controlled_document,
            text_nodes=context.text_nodes,
            resources=list(context.resources.values()),
            warnings=context.warnings,
        )

    @staticmethod
    def _read_page(page: Any) -> PdfPageData:
        lines = page.extract_text_lines(
            strip=True,
            return_chars=True,
            layout=True,
            x_tolerance=2,
            y_tolerance=3,
        )
        return PdfPageData(
            page_number=int(page.page_number),
            width=float(page.width),
            height=float(page.height),
            lines=list(lines or []),
            tables=list(page.find_tables() or []),
            images=list(page.images or []),
        )

    def _line_node(
        self,
        line: dict[str, Any],
        *,
        context: ParseContext,
        locator: str,
        page_number: int,
        median_body_size: float,
        region_kind: Literal["header", "footer", "paragraph"],
    ) -> ParagraphNode | HeadingNode:
        text = str(line.get("text") or "").strip()
        chars = list(line.get("chars") or [])
        font_sizes = [float(char.get("size", 0)) for char in chars if char.get("size")]
        font_size = statistics.median(font_sizes) if font_sizes else median_body_size
        font_name = str(chars[0].get("fontname") or "") if chars else None
        is_bold = bool(font_name and re.search(r"bold|black|heavy", font_name, re.IGNORECASE))
        node_id = context.node_id(locator)
        bbox = self._line_bbox(line)
        source = SourceNodeReference(source_node_id=locator, page_number=page_number)
        style = TextStyle(
            font_family=font_name or None,
            font_size=points(font_size),
            font_weight=700 if is_bold else None,
        )
        inline = TextNode(
            id=context.node_id(f"{locator}/text"),
            source=source,
            attributes={},
            text=text,
            style=style,
        )
        attributes: dict[str, JsonValue] = {"bbox": cast(JsonValue, bbox)}
        looks_like_heading = (
            region_kind == "paragraph"
            and len(text) <= 200
            and (font_size >= median_body_size * 1.2 or (is_bold and font_size > median_body_size))
        )
        kind: Literal["heading", "header", "footer", "paragraph"]
        if region_kind == "header":
            kind = "header"
        elif region_kind == "footer":
            kind = "footer"
        elif looks_like_heading:
            kind = "heading"
        else:
            kind = "paragraph"
        context.text_nodes.append(
            ParsedTextNode(
                node_id=node_id,
                kind=kind,
                page_number=page_number,
                text=text,
                metadata={
                    "bbox": cast(JsonValue, bbox),
                    "font_size_pt": round(font_size, 3),
                },
            )
        )
        if looks_like_heading:
            ratio = max(1.0, font_size / max(median_body_size, 1))
            level = 1 if ratio >= 1.8 else 2 if ratio >= 1.5 else 3
            return HeadingNode(
                id=node_id,
                source=source,
                attributes=attributes,
                level=level,
                content=[inline],
                style=ParagraphStyle(),
            )
        return ParagraphNode(
            id=node_id,
            source=source,
            attributes=attributes,
            content=[inline],
            style=ParagraphStyle(),
        )

    def _table_node(
        self,
        table: Any,
        *,
        context: ParseContext,
        locator: str,
        page_number: int,
    ) -> TableNode:
        extracted = table.extract() or []
        rows: list[TableRowNode] = []
        for row_index, values in enumerate(extracted):
            cells: list[TableCellNode] = []
            for cell_index, raw_value in enumerate(values or []):
                value = str(raw_value or "")
                cell_locator = f"{locator}/row/{row_index}/cell/{cell_index}"
                source = SourceNodeReference(source_node_id=cell_locator, page_number=page_number)
                paragraph = ParagraphNode(
                    id=context.node_id(f"{cell_locator}/paragraph"),
                    source=source,
                    attributes={},
                    content=(
                        [
                            TextNode(
                                id=context.node_id(f"{cell_locator}/text"),
                                source=source,
                                attributes={},
                                text=value,
                                style=TextStyle(),
                            )
                        ]
                        if value
                        else []
                    ),
                    style=ParagraphStyle(),
                )
                cells.append(
                    TableCellNode(
                        id=context.node_id(cell_locator),
                        source=source,
                        attributes={},
                        row_span=1,
                        column_span=1,
                        width=None,
                        style=TableCellStyle(),
                        blocks=[paragraph],
                    )
                )
                if value:
                    context.text_nodes.append(
                        ParsedTextNode(
                            node_id=context.node_id(cell_locator),
                            kind="table_cell",
                            page_number=page_number,
                            text=value,
                            metadata={"table_id": context.node_id(locator)},
                        )
                    )
            rows.append(
                TableRowNode(
                    id=context.node_id(f"{locator}/row/{row_index}"),
                    source=SourceNodeReference(
                        source_node_id=f"{locator}/row/{row_index}", page_number=page_number
                    ),
                    attributes={},
                    is_header=row_index == 0,
                    height=None,
                    cells=cells,
                )
            )
        bbox = [round(float(value), 3) for value in table.bbox]
        return TableNode(
            id=context.node_id(locator),
            source=SourceNodeReference(source_node_id=locator, page_number=page_number),
            attributes={"bbox": cast(JsonValue, bbox)},
            rows=rows,
            width=points(max(0, float(table.bbox[2]) - float(table.bbox[0]))),
            layout="fixed",
            repeat_header=False,
        )

    def _image_node(
        self,
        image: dict[str, Any],
        *,
        context: ParseContext,
        locator: str,
        page_number: int,
    ) -> ImageNode:
        left = float(image.get("x0", 0))
        right = float(image.get("x1", left))
        top = float(image.get("top", 0))
        bottom = float(image.get("bottom", top))
        bbox = [round(left, 3), round(top, 3), round(right, 3), round(bottom, 3)]
        resource_id = context.add_resource(
            locator=locator,
            media_type="application/x-docmind-pdf-image-reference",
            filename=f"page-{page_number}-image-reference.json",
            content_base64=None,
            raw_content=None,
            source_reference={
                "page_number": page_number,
                "left": left,
                "top": top,
                "right": right,
                "bottom": bottom,
            },
        )
        context.add_warning(
            "PDF_IMAGE_REFERENCE_ONLY",
            "PDF 图片位置已保留；模板资源将在页面渲染阶段裁切生成",  # noqa: RUF001
            severity="info",
            node_id=context.node_id(locator),
            page_number=page_number,
        )
        return ImageNode(
            id=context.node_id(locator),
            source=SourceNodeReference(source_node_id=locator, page_number=page_number),
            attributes={"bbox": cast(JsonValue, bbox)},
            resource_id=resource_id,
            alt_text="",
            title=None,
            width=points(max(0, right - left)),
            height=points(max(0, bottom - top)),
            alignment="left",
        )

    def _handle_ocr_page(
        self,
        pdf_content: bytes,
        *,
        context: ParseContext,
        page: PdfPageData,
        blocks: list[BlockNode],
        median_body_size: float,
    ) -> None:
        if not self._ocr_adapter.available:
            context.add_warning(
                "OCR_REQUIRED",
                "该页未提取到文本，需要启用受控 OCR 适配器",  # noqa: RUF001
                page_number=page.page_number,
            )
            return
        ocr_blocks = self._ocr_adapter.extract_page(
            pdf_content=pdf_content, page_number=page.page_number
        )
        for index, ocr_block in enumerate(ocr_blocks):
            line = {
                "text": ocr_block.text,
                "x0": ocr_block.left,
                "top": ocr_block.top,
                "x1": ocr_block.right,
                "bottom": ocr_block.bottom,
                "chars": [],
            }
            node = self._line_node(
                line,
                context=context,
                locator=f"pdf/page/{page.page_number}/ocr/{index}",
                page_number=page.page_number,
                median_body_size=median_body_size,
                region_kind="paragraph",
            )
            blocks.append(node)
        context.add_warning(
            "OCR_TEXT_USED",
            "该页使用 OCR 文本，证据坐标与置信度需要人工复核",  # noqa: RUF001
            severity="info",
            page_number=page.page_number,
        )

    @staticmethod
    def _line_bbox(line: dict[str, Any]) -> list[float]:
        return [
            round(float(line.get("x0", 0)), 3),
            round(float(line.get("top", 0)), 3),
            round(float(line.get("x1", 0)), 3),
            round(float(line.get("bottom", 0)), 3),
        ]

    @classmethod
    def _inside_bbox(cls, line: dict[str, Any], bbox: tuple[float, float, float, float]) -> bool:
        line_bbox = cls._line_bbox(line)
        center_x = (line_bbox[0] + line_bbox[2]) / 2
        center_y = (line_bbox[1] + line_bbox[3]) / 2
        return bbox[0] <= center_x <= bbox[2] and bbox[1] <= center_y <= bbox[3]

    @staticmethod
    def _bbox_tuple(values: Any) -> tuple[float, float, float, float]:
        return (float(values[0]), float(values[1]), float(values[2]), float(values[3]))

    @classmethod
    def _line_key(cls, line: dict[str, Any], page: PdfPageData) -> str:
        text = re.sub(r"\d+", "#", str(line.get("text") or "").strip().casefold())
        text = re.sub(r"\s+", " ", text)
        top = float(line.get("top", 0))
        zone = (
            "header"
            if top <= page.height * 0.12
            else "footer"
            if top >= page.height * 0.85
            else "body"
        )
        return f"{zone}:{text}"

    def _repeated_margin_keys(self, pages: list[PdfPageData]) -> tuple[set[str], set[str]]:
        if len(pages) < 2:
            return set(), set()
        counts: dict[str, set[int]] = {}
        for page in pages:
            for line in page.lines:
                key = self._line_key(line, page)
                if key.startswith("body:") or key.endswith(":"):
                    continue
                counts.setdefault(key, set()).add(page.page_number)
        threshold = max(2, int(len(pages) * 0.6 + 0.999))
        header_keys = {
            key
            for key, page_numbers in counts.items()
            if key.startswith("header:") and len(page_numbers) >= threshold
        }
        footer_keys = {
            key
            for key, page_numbers in counts.items()
            if key.startswith("footer:") and len(page_numbers) >= threshold
        }
        return header_keys, footer_keys

    def _body_font_sizes(self, pages: list[PdfPageData], excluded_keys: set[str]) -> list[float]:
        sizes: list[float] = []
        for page in pages:
            for line in page.lines:
                if self._line_key(line, page) in excluded_keys:
                    continue
                sizes.extend(
                    float(char["size"])
                    for char in line.get("chars", [])
                    if char.get("size") is not None
                )
        return sizes
