from __future__ import annotations

import base64
import re
from collections.abc import Iterable
from io import BytesIO
from pathlib import PurePosixPath
from typing import Literal, cast
from uuid import UUID
from zipfile import ZipFile

from docx import Document
from docx.document import Document as DocxDocument
from docx.oxml.ns import qn
from docx.section import _Footer, _Header
from docx.table import Table, _Cell
from docx.text.paragraph import Paragraph
from docx.text.run import Run
from lxml import etree  # type: ignore[import-untyped]
from pydantic import JsonValue

from docmind_ai.contracts.document import (
    BlockNode,
    ControlledDocument,
    ControlledDocumentMetadata,
    DocumentInsets,
    DynamicFieldNode,
    HeaderFooterRegion,
    HeaderFooterVariant,
    HeadingNode,
    ImageNode,
    InlineNode,
    LineBreakNode,
    ListItemNode,
    ListNode,
    PageBreakNode,
    PageLayout,
    ParagraphNode,
    ParagraphStyle,
    ParseDocumentResponse,
    ParsedTextNode,
    ParsedTextNodeKind,
    SourceNodeReference,
    TableCellNode,
    TableCellStyle,
    TableNode,
    TableOfContentsNode,
    TableRowNode,
    TabNode,
    TextNode,
    TextStyle,
)
from docmind_ai.parsing.common import ParseContext, default_page_layout, points
from docmind_ai.parsing.ooxml_security import validate_ooxml_archive

PARSER_VERSION = "docx-ooxml/1.0"
WORD_NAMESPACES = {"w": "http://schemas.openxmlformats.org/wordprocessingml/2006/main"}


class DocxParser:
    def parse(
        self,
        *,
        source_version_id: UUID,
        content: bytes,
        language: str,
        source_format: Literal["doc", "docx"],
    ) -> ParseDocumentResponse:
        validate_ooxml_archive(content)
        document = Document(BytesIO(content))
        context = ParseContext(source_version_id=source_version_id, language=language)
        current_page = [1]
        blocks = self._container_blocks(
            document,
            context=context,
            locator="body",
            text_kind="paragraph",
            current_page=current_page,
        )
        headers = self._header_footer_regions(
            document,
            context=context,
            region_type="header",
            current_page=current_page,
        )
        footers = self._header_footer_regions(
            document,
            context=context,
            region_type="footer",
            current_page=current_page,
        )
        self._extract_notes(content, context=context, note_type="footnote")
        self._extract_notes(content, context=context, note_type="endnote")

        if len(document.sections) > 1:
            context.add_warning(
                "MULTIPLE_SECTION_LAYOUTS",
                "文档包含多个节；中间模型以首节页面尺寸为主，并保留各节点源位置",  # noqa: RUF001
            )

        title = document.core_properties.title or ""
        resolved_language = self._document_language(document) or language
        controlled_document = ControlledDocument(
            root_id=context.node_id("root"),
            template_schema_version_id=None,
            metadata=ControlledDocumentMetadata(
                title=title,
                language=resolved_language,
                source_page_count=None,
            ),
            page_layout=self._page_layout(document),
            headers=headers,
            footers=footers,
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

    def _container_blocks(
        self,
        container: DocxDocument | _Header | _Footer | _Cell,
        *,
        context: ParseContext,
        locator: str,
        text_kind: ParsedTextNodeKind,
        current_page: list[int],
    ) -> list[BlockNode]:
        blocks: list[BlockNode] = []
        for index, item in enumerate(container.iter_inner_content()):
            item_locator = f"{locator}/{index}"
            if isinstance(item, Paragraph):
                blocks.extend(
                    self._paragraph_blocks(
                        item,
                        context=context,
                        locator=item_locator,
                        text_kind=text_kind,
                        current_page=current_page,
                    )
                )
            elif isinstance(item, Table):
                blocks.append(
                    self._table_node(
                        item,
                        context=context,
                        locator=item_locator,
                        text_kind=text_kind if text_kind in {"header", "footer"} else "table_cell",
                        current_page=current_page,
                    )
                )
        return blocks

    def _paragraph_blocks(
        self,
        paragraph: Paragraph,
        *,
        context: ParseContext,
        locator: str,
        text_kind: ParsedTextNodeKind,
        current_page: list[int],
    ) -> list[BlockNode]:
        node_id = context.node_id(locator)
        if paragraph.paragraph_format.page_break_before:
            current_page[0] += 1

        source = SourceNodeReference(source_node_id=locator, page_number=current_page[0])
        attributes: dict[str, JsonValue] = {}
        if paragraph.style is not None and paragraph.style.name:
            attributes["style_name"] = paragraph.style.name

        instructions = " ".join(
            element.text or "" for element in paragraph._p.xpath(".//w:instrText")
        ).strip()
        if re.search(r"(?:^|\s)TOC(?:\s|$)", instructions, flags=re.IGNORECASE):
            context.add_warning(
                "TOC_ENTRIES_REQUIRE_REFRESH",
                "目录字段已保留，但页码需要在最终渲染环境中刷新",  # noqa: RUF001
                node_id=node_id,
                page_number=current_page[0],
            )
            return [
                TableOfContentsNode(
                    id=node_id,
                    source=source,
                    attributes=attributes,
                    title=paragraph.text.strip(),
                    entries=[],
                    auto_update=True,
                )
            ]

        inline_content = self._inline_nodes(
            paragraph,
            context=context,
            locator=locator,
            source=source,
        )
        paragraph_style = self._paragraph_style(paragraph)
        heading_level = self._heading_level(paragraph)
        if heading_level is not None:
            primary_block: BlockNode = HeadingNode(
                id=node_id,
                source=source,
                attributes=attributes,
                level=heading_level,
                content=inline_content,
                style=paragraph_style,
            )
            effective_kind: ParsedTextNodeKind = (
                text_kind if text_kind in {"header", "footer"} else "heading"
            )
        else:
            paragraph_node = ParagraphNode(
                id=node_id,
                source=source,
                attributes=attributes,
                content=inline_content,
                style=paragraph_style,
            )
            if self._is_list_paragraph(paragraph):
                item_id = context.node_id(f"{locator}/list-item")
                primary_block = ListNode(
                    id=context.node_id(f"{locator}/list"),
                    source=source,
                    attributes={"source_style": attributes.get("style_name", "")},
                    ordered=self._is_ordered_list(paragraph),
                    start=1,
                    marker="decimal" if self._is_ordered_list(paragraph) else "bullet",
                    items=[
                        ListItemNode(
                            id=item_id,
                            source=source,
                            attributes={},
                            blocks=[paragraph_node],
                        )
                    ],
                )
                effective_kind = text_kind if text_kind in {"header", "footer"} else "list_item"
            else:
                primary_block = paragraph_node
                effective_kind = text_kind

        if paragraph.text:
            context.text_nodes.append(
                ParsedTextNode(
                    node_id=node_id,
                    kind=effective_kind,
                    page_number=current_page[0],
                    text=paragraph.text,
                    metadata={"style_name": attributes.get("style_name", "")},
                )
            )

        result: list[BlockNode] = [primary_block]
        result.extend(
            self._image_nodes(
                paragraph,
                context=context,
                locator=locator,
                source=source,
            )
        )
        explicit_page_breaks = len(paragraph._p.xpath('.//w:br[@w:type="page"]'))
        rendered_page_breaks = len(paragraph._p.xpath(".//w:lastRenderedPageBreak"))
        for break_index in range(max(explicit_page_breaks, rendered_page_breaks)):
            result.append(
                PageBreakNode(
                    id=context.node_id(f"{locator}/page-break/{break_index}"),
                    source=source,
                    attributes={},
                )
            )
            current_page[0] += 1
        return result

    def _inline_nodes(
        self,
        paragraph: Paragraph,
        *,
        context: ParseContext,
        locator: str,
        source: SourceNodeReference,
    ) -> list[InlineNode]:
        nodes: list[InlineNode] = []
        run_index = 0
        for child in paragraph.iter_inner_content():
            runs: Iterable[Run]
            runs = [child] if isinstance(child, Run) else child.runs
            for run in runs:
                run_locator = f"{locator}/run/{run_index}"
                run_index += 1
                instruction = " ".join(
                    element.text or "" for element in run._r.xpath(".//w:instrText")
                ).strip()
                dynamic_field = self._dynamic_field(instruction)
                if dynamic_field is not None:
                    nodes.append(
                        DynamicFieldNode(
                            id=context.node_id(run_locator),
                            source=source,
                            attributes={},
                            field=dynamic_field,
                            format=instruction[:100] or None,
                            style=self._text_style(run),
                        )
                    )
                    continue
                pieces = re.split(r"(\n|\t)", run.text)
                for piece_index, piece in enumerate(pieces):
                    piece_locator = f"{run_locator}/{piece_index}"
                    if piece == "\n":
                        nodes.append(
                            LineBreakNode(
                                id=context.node_id(piece_locator), source=source, attributes={}
                            )
                        )
                    elif piece == "\t":
                        nodes.append(
                            TabNode(id=context.node_id(piece_locator), source=source, attributes={})
                        )
                    elif piece:
                        nodes.append(
                            TextNode(
                                id=context.node_id(piece_locator),
                                source=source,
                                attributes={},
                                text=piece,
                                style=self._text_style(run),
                            )
                        )
        if not nodes and paragraph.text:
            nodes.append(
                TextNode(
                    id=context.node_id(f"{locator}/text"),
                    source=source,
                    attributes={},
                    text=paragraph.text,
                    style=TextStyle(),
                )
            )
        return nodes

    def _image_nodes(
        self,
        paragraph: Paragraph,
        *,
        context: ParseContext,
        locator: str,
        source: SourceNodeReference,
    ) -> list[ImageNode]:
        result: list[ImageNode] = []
        for image_index, blip in enumerate(paragraph._p.xpath(".//a:blip")):
            relationship_id = blip.get(qn("r:embed"))
            if not relationship_id or relationship_id not in paragraph.part.related_parts:
                context.add_warning(
                    "DOCX_IMAGE_RELATIONSHIP_MISSING",
                    "图片关系无法解析，已保留结构告警",  # noqa: RUF001
                    node_id=context.node_id(locator),
                    page_number=source.page_number,
                )
                continue
            image_part = paragraph.part.related_parts[relationship_id]
            raw_content = bytes(image_part.blob)
            filename = PurePosixPath(str(image_part.partname)).name or f"image-{image_index}"
            resource_id = context.add_resource(
                locator=f"{locator}/image/{image_index}",
                media_type=str(image_part.content_type),
                filename=filename,
                content_base64=base64.b64encode(raw_content).decode("ascii"),
                raw_content=raw_content,
                source_reference={"relationship_id": relationship_id},
            )
            extent = paragraph._p.xpath(".//wp:extent")
            width = points(72)
            height = points(72)
            if extent:
                width = points(
                    int(extent[min(image_index, len(extent) - 1)].get("cx", "914400")) / 12700
                )
                height = points(
                    int(extent[min(image_index, len(extent) - 1)].get("cy", "914400")) / 12700
                )
            result.append(
                ImageNode(
                    id=context.node_id(f"{locator}/image/{image_index}"),
                    source=source,
                    attributes={"relationship_id": relationship_id},
                    resource_id=resource_id,
                    alt_text="",
                    title=None,
                    width=width,
                    height=height,
                    alignment=cast(
                        Literal["left", "center", "right"],
                        (
                            self._paragraph_alignment(paragraph)
                            if self._paragraph_alignment(paragraph) in {"left", "center", "right"}
                            else "left"
                        ),
                    ),
                )
            )
        return result

    def _table_node(
        self,
        table: Table,
        *,
        context: ParseContext,
        locator: str,
        text_kind: ParsedTextNodeKind,
        current_page: list[int],
    ) -> TableNode:
        rows: list[TableRowNode] = []
        for row_index, row in enumerate(table.rows):
            cells: list[TableCellNode] = []
            seen_cells: set[int] = set()
            for cell_index, cell in enumerate(row.cells):
                cell_identity = id(cell._tc)
                if cell_identity in seen_cells:
                    continue
                seen_cells.add(cell_identity)
                cell_locator = f"{locator}/row/{row_index}/cell/{cell_index}"
                grid_span = 1
                if cell._tc.tcPr is not None and cell._tc.tcPr.gridSpan is not None:
                    grid_span = int(cell._tc.tcPr.gridSpan.val)
                cells.append(
                    TableCellNode(
                        id=context.node_id(cell_locator),
                        source=SourceNodeReference(
                            source_node_id=cell_locator, page_number=current_page[0]
                        ),
                        attributes={},
                        row_span=1,
                        column_span=max(1, grid_span),
                        width=points(cell.width.pt) if cell.width is not None else None,
                        style=self._table_cell_style(cell),
                        blocks=self._container_blocks(
                            cell,
                            context=context,
                            locator=cell_locator,
                            text_kind=text_kind,
                            current_page=current_page,
                        ),
                    )
                )
            is_header = bool(row._tr.xpath("./w:trPr/w:tblHeader"))
            rows.append(
                TableRowNode(
                    id=context.node_id(f"{locator}/row/{row_index}"),
                    source=SourceNodeReference(
                        source_node_id=f"{locator}/row/{row_index}", page_number=current_page[0]
                    ),
                    attributes={},
                    is_header=is_header,
                    height=points(row.height.pt) if row.height is not None else None,
                    cells=cells,
                )
            )
        return TableNode(
            id=context.node_id(locator),
            source=SourceNodeReference(source_node_id=locator, page_number=current_page[0]),
            attributes={"style_name": table.style.name if table.style is not None else ""},
            rows=rows,
            width=None,
            layout="fixed" if table.autofit is False else "auto",
            repeat_header=any(row.is_header for row in rows),
        )

    def _header_footer_regions(
        self,
        document: DocxDocument,
        *,
        context: ParseContext,
        region_type: Literal["header", "footer"],
        current_page: list[int],
    ) -> list[HeaderFooterRegion]:
        regions: list[HeaderFooterRegion] = []
        seen_parts: set[tuple[str, str]] = set()
        variants: tuple[tuple[HeaderFooterVariant, str], ...] = (
            ("default", region_type),
            ("first_page", f"first_page_{region_type}"),
            ("even_pages", f"even_page_{region_type}"),
        )
        for section_index, section in enumerate(document.sections):
            for variant, attribute_name in variants:
                region = getattr(section, attribute_name)
                part_name = str(region.part.partname)
                identity = (variant, part_name)
                if identity in seen_parts:
                    continue
                seen_parts.add(identity)
                blocks = self._container_blocks(
                    region,
                    context=context,
                    locator=f"{region_type}/{section_index}/{variant}",
                    text_kind=region_type,
                    current_page=[1],
                )
                if blocks and any(self._block_has_content(block) for block in blocks):
                    regions.append(HeaderFooterRegion(variant=variant, blocks=blocks))
        return regions

    def _extract_notes(
        self,
        content: bytes,
        *,
        context: ParseContext,
        note_type: Literal["footnote", "endnote"],
    ) -> None:
        archive_path = f"word/{note_type}s.xml"
        with ZipFile(BytesIO(content)) as archive:
            if archive_path not in archive.namelist():
                return
            parser = etree.XMLParser(resolve_entities=False, no_network=True, huge_tree=False)
            root = etree.fromstring(archive.read(archive_path), parser=parser)
        note_tag = f"{{{WORD_NAMESPACES['w']}}}{note_type}"
        paragraph_tag = f"{{{WORD_NAMESPACES['w']}}}p"
        text_tag = f"{{{WORD_NAMESPACES['w']}}}t"
        id_attribute = f"{{{WORD_NAMESPACES['w']}}}id"
        found = False
        for note in root.iter(note_tag):
            note_id = int(note.get(id_attribute, "-1"))
            if note_id < 0:
                continue
            for paragraph_index, paragraph in enumerate(note.iter(paragraph_tag)):
                text = "".join(element.text or "" for element in paragraph.iter(text_tag))
                if not text:
                    continue
                found = True
                locator = f"{note_type}/{note_id}/{paragraph_index}"
                context.text_nodes.append(
                    ParsedTextNode(
                        node_id=context.node_id(locator),
                        kind=note_type,
                        page_number=None,
                        text=text,
                        metadata={"note_id": note_id},
                    )
                )
        if found:
            context.add_warning(
                "NOTE_RENDERING_LIMITED",
                "脚注或尾注文本已保留用于抽取，但其精确页面锚点需要最终排版引擎确认",  # noqa: RUF001
                severity="info",
            )

    @staticmethod
    def _dynamic_field(
        instruction: str,
    ) -> Literal["page_number", "page_count", "current_date"] | None:
        normalized = instruction.upper().strip()
        if re.search(r"(?:^|\s)NUMPAGES(?:\s|$)", normalized):
            return "page_count"
        if re.search(r"(?:^|\s)PAGE(?:\s|$)", normalized):
            return "page_number"
        if re.search(r"(?:^|\s)(?:DATE|CREATEDATE|SAVEDATE)(?:\s|$)", normalized):
            return "current_date"
        return None

    @staticmethod
    def _heading_level(paragraph: Paragraph) -> int | None:
        style_name = paragraph.style.name if paragraph.style is not None else ""
        match = re.search(r"(?:Heading|标题)\s*([1-6])", style_name, flags=re.IGNORECASE)
        if match:
            return int(match.group(1))
        outline_levels = paragraph._p.xpath("./w:pPr/w:outlineLvl")
        if outline_levels:
            value = outline_levels[0].get(qn("w:val"))
            if value is not None and value.isdigit() and int(value) <= 5:
                return int(value) + 1
        return None

    @staticmethod
    def _is_list_paragraph(paragraph: Paragraph) -> bool:
        if paragraph._p.pPr is not None and paragraph._p.pPr.numPr is not None:
            return True
        style_name = paragraph.style.name if paragraph.style is not None else ""
        return "list" in style_name.lower() or "列表" in style_name

    @staticmethod
    def _is_ordered_list(paragraph: Paragraph) -> bool:
        style_name = paragraph.style.name.lower() if paragraph.style is not None else ""
        return any(marker in style_name for marker in ("number", "decimal", "编号"))

    @staticmethod
    def _paragraph_alignment(
        paragraph: Paragraph,
    ) -> Literal["left", "center", "right", "justify"] | None:
        alignment = paragraph.alignment
        if alignment is None:
            return None
        name = getattr(alignment, "name", str(alignment)).lower()
        if "center" in name:
            return "center"
        if "right" in name:
            return "right"
        if "justify" in name or "distribute" in name:
            return "justify"
        return "left"

    def _paragraph_style(self, paragraph: Paragraph) -> ParagraphStyle:
        formatting = paragraph.paragraph_format
        line_height = None
        if formatting.line_spacing is not None and isinstance(formatting.line_spacing, float):
            line_height = formatting.line_spacing
        return ParagraphStyle(
            alignment=self._paragraph_alignment(paragraph),
            line_height=line_height,
            spacing_before=(
                points(formatting.space_before.pt) if formatting.space_before is not None else None
            ),
            spacing_after=(
                points(formatting.space_after.pt) if formatting.space_after is not None else None
            ),
            first_line_indent=(
                points(formatting.first_line_indent.pt)
                if formatting.first_line_indent is not None
                else None
            ),
            left_indent=(
                points(formatting.left_indent.pt) if formatting.left_indent is not None else None
            ),
            right_indent=(
                points(formatting.right_indent.pt) if formatting.right_indent is not None else None
            ),
            keep_with_next=formatting.keep_with_next,
            keep_lines_together=formatting.keep_together,
            page_break_before=formatting.page_break_before,
        )

    @staticmethod
    def _text_style(run: Run) -> TextStyle:
        color = None
        if run.font.color is not None and run.font.color.rgb is not None:
            color = f"#{run.font.color.rgb}"
        vertical_align = None
        if run.font.superscript:
            vertical_align = "superscript"
        elif run.font.subscript:
            vertical_align = "subscript"
        decorations: list[Literal["underline", "line_through"]] = []
        if run.underline:
            decorations.append("underline")
        if run.font.strike:
            decorations.append("line_through")
        return TextStyle(
            font_family=run.font.name,
            font_size=points(run.font.size.pt) if run.font.size is not None else None,
            font_weight=700 if run.bold else None,
            italic=run.italic,
            decorations=decorations or None,
            color=color,
            vertical_align=vertical_align,  # type: ignore[arg-type]
        )

    @staticmethod
    def _table_cell_style(cell: _Cell) -> TableCellStyle:
        background = None
        shading = cell._tc.xpath("./w:tcPr/w:shd")
        if shading:
            fill = shading[0].get(qn("w:fill"))
            if fill and fill.lower() not in {"auto", "nil"}:
                background = f"#{fill}"
        vertical = None
        if cell.vertical_alignment is not None:
            name = getattr(cell.vertical_alignment, "name", str(cell.vertical_alignment)).lower()
            vertical = "middle" if "center" in name else "bottom" if "bottom" in name else "top"
        return TableCellStyle(
            background_color=background,
            vertical_align=vertical,  # type: ignore[arg-type]
        )

    @staticmethod
    def _page_layout(document: DocxDocument) -> PageLayout:
        if not document.sections:
            return default_page_layout()
        section = document.sections[0]
        if section.page_width is None or section.page_height is None:
            return default_page_layout()
        width = section.page_width.pt
        height = section.page_height.pt
        layout = default_page_layout(width=width, height=height)
        return layout.model_copy(
            update={
                "margins": DocumentInsets(
                    top=points(section.top_margin.pt)
                    if section.top_margin is not None
                    else points(72),
                    right=(
                        points(section.right_margin.pt)
                        if section.right_margin is not None
                        else points(72)
                    ),
                    bottom=(
                        points(section.bottom_margin.pt)
                        if section.bottom_margin is not None
                        else points(72)
                    ),
                    left=points(section.left_margin.pt)
                    if section.left_margin is not None
                    else points(72),
                ),
                "header_distance": (
                    points(section.header_distance.pt)
                    if section.header_distance is not None
                    else points(36)
                ),
                "footer_distance": (
                    points(section.footer_distance.pt)
                    if section.footer_distance is not None
                    else points(36)
                ),
            }
        )

    @staticmethod
    def _document_language(document: DocxDocument) -> str | None:
        languages = document.styles.element.xpath(".//w:rPrDefault/w:rPr/w:lang")
        if not languages:
            return None
        value = languages[0].get(qn("w:val"))
        return cast(str | None, value)

    @staticmethod
    def _block_has_content(block: BlockNode) -> bool:
        if isinstance(block, (ParagraphNode, HeadingNode)):
            return bool(block.content)
        if isinstance(block, TableNode):
            return bool(block.rows)
        return True
