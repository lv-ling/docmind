from typing import Literal
from uuid import UUID

from pydantic import Field, JsonValue

from docmind_ai.contracts.common import StrictModel

DocumentFormat = Literal["doc", "docx", "pdf"]
DocumentLengthUnit = Literal["pt", "px", "mm", "cm", "in", "percent"]
ParagraphAlignment = Literal["left", "center", "right", "justify"]
HeaderFooterVariant = Literal["default", "first_page", "even_pages"]
ConversionWarningSeverity = Literal["info", "warning", "error"]
ParsedTextNodeKind = Literal[
    "paragraph",
    "heading",
    "list_item",
    "table_cell",
    "header",
    "footer",
    "footnote",
    "endnote",
]


class DocumentLength(StrictModel):
    value: float
    unit: DocumentLengthUnit


class DocumentInsets(StrictModel):
    top: DocumentLength
    right: DocumentLength
    bottom: DocumentLength
    left: DocumentLength


class PageLayout(StrictModel):
    size: Literal["a4", "a3", "letter", "legal", "custom"]
    orientation: Literal["portrait", "landscape"]
    width: DocumentLength
    height: DocumentLength
    margins: DocumentInsets
    header_distance: DocumentLength
    footer_distance: DocumentLength


class TextStyle(StrictModel):
    font_family: str | None = None
    font_size: DocumentLength | None = None
    font_weight: int | None = Field(default=None, ge=1, le=1000)
    italic: bool | None = None
    decorations: list[Literal["underline", "line_through"]] | None = None
    color: str | None = None
    background_color: str | None = None
    letter_spacing: DocumentLength | None = None
    vertical_align: Literal["baseline", "subscript", "superscript"] | None = None


class ParagraphStyle(StrictModel):
    alignment: ParagraphAlignment | None = None
    line_height: float | None = Field(default=None, gt=0)
    spacing_before: DocumentLength | None = None
    spacing_after: DocumentLength | None = None
    first_line_indent: DocumentLength | None = None
    left_indent: DocumentLength | None = None
    right_indent: DocumentLength | None = None
    keep_with_next: bool | None = None
    keep_lines_together: bool | None = None
    page_break_before: bool | None = None


class BorderStyle(StrictModel):
    width: DocumentLength
    style: Literal["none", "solid", "dashed", "dotted", "double"]
    color: str


class TableCellStyle(StrictModel):
    background_color: str | None = None
    vertical_align: Literal["top", "middle", "bottom"] | None = None
    padding: DocumentInsets | None = None
    borders: dict[Literal["top", "right", "bottom", "left"], BorderStyle] | None = None


class SourceNodeReference(StrictModel):
    source_node_id: str = Field(min_length=1, max_length=255)
    page_number: int | None = Field(default=None, ge=1)


class DocumentNodeBase(StrictModel):
    id: str = Field(min_length=1, max_length=255)
    source: SourceNodeReference | None = None
    attributes: dict[str, JsonValue] = Field(default_factory=dict)


class TextNode(DocumentNodeBase):
    type: Literal["text"] = "text"
    text: str = Field(max_length=1_000_000)
    style: TextStyle = Field(default_factory=TextStyle)


class LineBreakNode(DocumentNodeBase):
    type: Literal["line_break"] = "line_break"


class TabNode(DocumentNodeBase):
    type: Literal["tab"] = "tab"


class DynamicFieldNode(DocumentNodeBase):
    type: Literal["dynamic_field"] = "dynamic_field"
    field: Literal["page_number", "page_count", "current_date"]
    format: str | None = Field(default=None, max_length=100)
    style: TextStyle = Field(default_factory=TextStyle)


InlineNode = TextNode | LineBreakNode | TabNode | DynamicFieldNode


class ParagraphNode(DocumentNodeBase):
    type: Literal["paragraph"] = "paragraph"
    content: list[InlineNode] = Field(default_factory=list)
    style: ParagraphStyle = Field(default_factory=ParagraphStyle)


class HeadingNode(DocumentNodeBase):
    type: Literal["heading"] = "heading"
    level: int = Field(ge=1, le=6)
    content: list[InlineNode] = Field(default_factory=list)
    style: ParagraphStyle = Field(default_factory=ParagraphStyle)


class ListItemNode(DocumentNodeBase):
    type: Literal["list_item"] = "list_item"
    blocks: list["BlockNode"] = Field(default_factory=list)


class ListNode(DocumentNodeBase):
    type: Literal["list"] = "list"
    ordered: bool
    start: int = Field(default=1, ge=1)
    marker: Literal["decimal", "lower_alpha", "upper_alpha", "lower_roman", "upper_roman", "bullet"]
    items: list[ListItemNode] = Field(default_factory=list)


class TableCellNode(DocumentNodeBase):
    type: Literal["table_cell"] = "table_cell"
    row_span: int = Field(default=1, ge=1)
    column_span: int = Field(default=1, ge=1)
    width: DocumentLength | None = None
    style: TableCellStyle = Field(default_factory=TableCellStyle)
    blocks: list["BlockNode"] = Field(default_factory=list)


class TableRowNode(DocumentNodeBase):
    type: Literal["table_row"] = "table_row"
    is_header: bool = False
    height: DocumentLength | None = None
    cells: list[TableCellNode] = Field(default_factory=list)


class TableNode(DocumentNodeBase):
    type: Literal["table"] = "table"
    rows: list[TableRowNode] = Field(default_factory=list)
    width: DocumentLength | None = None
    layout: Literal["auto", "fixed"] = "auto"
    repeat_header: bool = False


class ImageNode(DocumentNodeBase):
    type: Literal["image"] = "image"
    resource_id: UUID
    alt_text: str = Field(default="", max_length=500)
    title: str | None = Field(default=None, max_length=500)
    width: DocumentLength
    height: DocumentLength
    alignment: Literal["left", "center", "right"] = "left"


class TableOfContentsEntry(StrictModel):
    heading_node_id: str | None = Field(default=None, max_length=255)
    level: int = Field(ge=1, le=9)
    label: str = Field(max_length=1000)
    page_number: int | None = Field(default=None, ge=1)


class TableOfContentsNode(DocumentNodeBase):
    type: Literal["table_of_contents"] = "table_of_contents"
    title: str = Field(default="", max_length=500)
    entries: list[TableOfContentsEntry] = Field(default_factory=list)
    auto_update: bool = True


class PageBreakNode(DocumentNodeBase):
    type: Literal["page_break"] = "page_break"


class PageMarkerNode(DocumentNodeBase):
    type: Literal["page_marker"] = "page_marker"
    page_number: int = Field(ge=1)


BlockNode = (
    ParagraphNode
    | HeadingNode
    | ListNode
    | TableNode
    | ImageNode
    | TableOfContentsNode
    | PageBreakNode
    | PageMarkerNode
)


class HeaderFooterRegion(StrictModel):
    variant: HeaderFooterVariant
    blocks: list[BlockNode] = Field(default_factory=list)


class ControlledDocumentMetadata(StrictModel):
    title: str = Field(default="", max_length=1000)
    language: str = Field(min_length=1, max_length=35)
    source_page_count: int | None = Field(default=None, ge=1)


class ControlledDocument(StrictModel):
    model_version: Literal["1.0"] = "1.0"
    root_id: str = Field(min_length=1, max_length=255)
    template_schema_version_id: UUID | None = None
    metadata: ControlledDocumentMetadata
    page_layout: PageLayout
    headers: list[HeaderFooterRegion] = Field(default_factory=list)
    footers: list[HeaderFooterRegion] = Field(default_factory=list)
    blocks: list[BlockNode] = Field(default_factory=list)


class ParsedTextNode(StrictModel):
    node_id: str = Field(min_length=1, max_length=255)
    kind: ParsedTextNodeKind
    page_number: int | None = Field(default=None, ge=1)
    text: str = Field(max_length=1_000_000)
    metadata: dict[str, JsonValue] = Field(default_factory=dict)


class ParsedDocumentResource(StrictModel):
    id: UUID
    media_type: str = Field(min_length=1, max_length=255)
    filename: str = Field(min_length=1, max_length=255)
    sha256: str = Field(pattern=r"^[a-f0-9]{64}$")
    byte_length: int = Field(ge=0)
    content_base64: str | None = None
    source_reference: dict[str, JsonValue] = Field(default_factory=dict)


class ConversionWarning(StrictModel):
    code: str = Field(min_length=1, max_length=100)
    severity: ConversionWarningSeverity
    message: str = Field(min_length=1, max_length=500)
    node_id: str | None = Field(default=None, max_length=255)
    page_number: int | None = Field(default=None, ge=1)


class ParseDocumentResponse(StrictModel):
    source_version_id: UUID
    source_format: DocumentFormat
    parser_version: str = Field(min_length=1, max_length=100)
    document: ControlledDocument
    text_nodes: list[ParsedTextNode] = Field(max_length=100_000)
    resources: list[ParsedDocumentResource] = Field(max_length=10_000)
    warnings: list[ConversionWarning] = Field(max_length=10_000)


ListItemNode.model_rebuild()
TableCellNode.model_rebuild()
