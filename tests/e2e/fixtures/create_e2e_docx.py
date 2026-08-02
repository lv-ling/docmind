#!/usr/bin/env python3
"""Create the deterministic DOCX fixture using only the Python standard library."""

from __future__ import annotations

import sys
from html import escape
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile

CONTENT_TYPES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
  <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
  <Override PartName="/word/header1.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.header+xml"/>
  <Override PartName="/word/footer1.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.footer+xml"/>
</Types>
"""

PACKAGE_RELATIONSHIPS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>
"""

DOCUMENT_RELATIONSHIPS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/header" Target="header1.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/footer" Target="footer1.xml"/>
</Relationships>
"""

STYLES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:docDefaults>
    <w:rPrDefault><w:rPr><w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:eastAsia="Arial Unicode MS"/><w:sz w:val="22"/><w:color w:val="242A33"/></w:rPr></w:rPrDefault>
    <w:pPrDefault><w:pPr><w:spacing w:after="120" w:line="264" w:lineRule="auto"/></w:pPr></w:pPrDefault>
  </w:docDefaults>
  <w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/></w:style>
  <w:style w:type="paragraph" w:styleId="Heading1"><w:name w:val="heading 1"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:qFormat/><w:pPr><w:keepNext/><w:spacing w:before="320" w:after="160"/></w:pPr><w:rPr><w:b/><w:color w:val="2E74B5"/><w:sz w:val="32"/></w:rPr></w:style>
  <w:style w:type="paragraph" w:styleId="Heading2"><w:name w:val="heading 2"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:qFormat/><w:pPr><w:keepNext/><w:spacing w:before="240" w:after="120"/></w:pPr><w:rPr><w:b/><w:color w:val="2E74B5"/><w:sz w:val="26"/></w:rPr></w:style>
</w:styles>
"""

HEADER = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:hdr xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:p><w:r><w:rPr><w:b/><w:color w:val="646B75"/><w:sz w:val="18"/></w:rPr><w:t>DOCMIND 联调样例    内部测试 · 请勿外传</w:t></w:r></w:p></w:hdr>
"""

FOOTER = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:ftr xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:p><w:pPr><w:jc w:val="right"/></w:pPr><w:r><w:t xml:space="preserve">第 </w:t></w:r><w:r><w:fldChar w:fldCharType="begin"/></w:r><w:r><w:instrText xml:space="preserve"> PAGE </w:instrText></w:r><w:r><w:fldChar w:fldCharType="separate"/></w:r><w:r><w:t>1</w:t></w:r><w:r><w:fldChar w:fldCharType="end"/></w:r><w:r><w:t xml:space="preserve"> 页</w:t></w:r></w:p></w:ftr>
"""


def run(
    text: str,
    *,
    bold: bool = False,
    italic: bool = False,
    color: str | None = None,
    size: int | None = None,
) -> str:
    properties = [
        '<w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:eastAsia="Arial Unicode MS"/>'
    ]
    if bold:
        properties.append("<w:b/>")
    if italic:
        properties.append("<w:i/>")
    if color:
        properties.append(f'<w:color w:val="{color}"/>')
    if size:
        properties.append(f'<w:sz w:val="{size}"/>')
    return (
        "<w:r><w:rPr>"
        + "".join(properties)
        + f'</w:rPr><w:t xml:space="preserve">{escape(text)}</w:t></w:r>'
    )


def paragraph(
    *runs: str,
    style: str | None = None,
    before: int | None = None,
    after: int | None = None,
) -> str:
    properties: list[str] = []
    if style:
        properties.append(f'<w:pStyle w:val="{style}"/>')
    if before is not None or after is not None:
        attributes = []
        if before is not None:
            attributes.append(f'w:before="{before}"')
        if after is not None:
            attributes.append(f'w:after="{after}"')
        properties.append(f"<w:spacing {' '.join(attributes)}/>")
    ppr = f"<w:pPr>{''.join(properties)}</w:pPr>" if properties else ""
    return f"<w:p>{ppr}{''.join(runs)}</w:p>"


def text_paragraph(text: str, *, style: str | None = None) -> str:
    return paragraph(run(text), style=style)


def table(rows: list[tuple[str, ...]], widths: tuple[int, ...]) -> str:
    grid = "".join(f'<w:gridCol w:w="{width}"/>' for width in widths)
    rendered_rows = []
    for row_index, values in enumerate(rows):
        cells = []
        for column_index, value in enumerate(values):
            shading = '<w:shd w:fill="F2F4F7"/>' if row_index == 0 else ""
            cell_run = run(value, bold=row_index == 0, size=20)
            cells.append(
                f'<w:tc><w:tcPr><w:tcW w:w="{widths[column_index]}" w:type="dxa"/>{shading}</w:tcPr>'
                f"{paragraph(cell_run, after=0)}</w:tc>"
            )
        rendered_rows.append(f"<w:tr>{''.join(cells)}</w:tr>")
    return (
        '<w:tbl><w:tblPr><w:tblW w:w="9360" w:type="dxa"/>'
        '<w:tblBorders><w:top w:val="single" w:sz="4" w:color="B7BCC2"/>'
        '<w:left w:val="single" w:sz="4" w:color="B7BCC2"/>'
        '<w:bottom w:val="single" w:sz="4" w:color="B7BCC2"/>'
        '<w:right w:val="single" w:sz="4" w:color="B7BCC2"/>'
        '<w:insideH w:val="single" w:sz="4" w:color="B7BCC2"/>'
        '<w:insideV w:val="single" w:sz="4" w:color="B7BCC2"/></w:tblBorders></w:tblPr>'
        f"<w:tblGrid>{grid}</w:tblGrid>{''.join(rendered_rows)}</w:tbl>"
    )


def document_xml() -> str:
    body = [
        paragraph(run("跨境供应商尽调简报", bold=True, size=46), before=320, after=80),
        paragraph(
            run(
                "用于验证 DOCX 解析、版式复刻、敏感信息占位与模板版本管理",
                color="646B75",
                size=24,
            ),
            after=320,
        ),
    ]
    for label, value in (
        ("文档类型", "跨境供应商尽调简报"),
        ("负责人", "林晓岚"),
        ("日期", "2026 年 8 月 1 日"),
        ("状态", "待审核"),
    ):
        body.append(paragraph(run(f"{label}：", bold=True), run(value), after=40))

    body.extend(
        [
            text_paragraph("一、审查摘要", style="Heading1"),
            text_paragraph(
                "本简报包含标题、页眉页脚、分级标题、混合样式、表格和多国家联系信息，用于验证系统在真实基础设施下能够保存原件、生成安全 HTML，并保持可编辑结构。"
            ),
            paragraph(
                run("重点结论：", bold=True),
                run(
                    "供应商资料完整，但所有个人信息在发送给模型前必须完成脱敏，并在返回后按映射恢复。"
                ),
            ),
            text_paragraph("二、联系人信息", style="Heading1"),
            paragraph(
                run(
                    "表 1 · 本地联调虚构数据，仅用于自动化测试",
                    italic=True,
                    color="646B75",
                    size=18,
                ),
                before=80,
                after=80,
            ),
            paragraph(run("联系人姓名：", bold=True), run("张伟"), after=40),
            paragraph(
                run("联系人电话：", bold=True), run("+86 138 0013 8000"), after=40
            ),
            paragraph(
                run("联系人电子邮箱：", bold=True),
                run("zhang.wei@example.cn"),
                after=40,
            ),
            table(
                [
                    ("国家/地区", "联系人", "联系信息"),
                    (
                        "中国",
                        "张伟",
                        "+86 138 0013 8000；zhang.wei@example.cn；11010519491231002X",
                    ),
                    (
                        "美国",
                        "Emily Carter",
                        "+1 (415) 555-0138；emily.carter@example.com",
                    ),
                    ("日本", "佐藤健", "+81 90-1234-5678；sato.ken@example.jp"),
                    ("德国", "Anna Müller", "+49 30 12345678；anna.mueller@example.de"),
                ],
                (1500, 2100, 5760),
            ),
            '<w:p><w:r><w:br w:type="page"/></w:r></w:p>',
            text_paragraph("三、审查记录", style="Heading1"),
            text_paragraph("3.1 合规要求", style="Heading2"),
            text_paragraph(
                "应用层需要识别中国、美国、日本、韩国、德国、法国、英国、澳大利亚和荷兰等地区的常见 PII 格式。"
            ),
            text_paragraph("3.2 模板要求", style="Heading2"),
            paragraph(
                run("版式复刻", bold=True, color="2E74B5"),
                run("应保留页面、段落、字号、对齐、页眉页脚与表格结构；"),
                run("编辑版本", bold=True, color="2E74B5"),
                run("必须可追溯、可发布并可回滚。"),
            ),
            text_paragraph("四、审批", style="Heading1"),
            table(
                [("角色", "姓名", "结论"), ("合规负责人", "王敏", "通过，待模板复核")],
                (2200, 2200, 4960),
            ),
        ]
    )

    body.append(
        '<w:sectPr><w:headerReference w:type="default" r:id="rId2"/>'
        '<w:footerReference w:type="default" r:id="rId3"/>'
        '<w:pgSz w:w="12240" w:h="15840"/><w:pgMar w:top="1440" w:right="1440" '
        'w:bottom="1440" w:left="1440" w:header="708" w:footer="708" w:gutter="0"/></w:sectPr>'
    )
    return (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" '
        'xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">'
        f"<w:body>{''.join(body)}</w:body></w:document>"
    )


def build(output: Path) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    with ZipFile(output, "w", compression=ZIP_DEFLATED) as archive:
        archive.writestr("[Content_Types].xml", CONTENT_TYPES)
        archive.writestr("_rels/.rels", PACKAGE_RELATIONSHIPS)
        archive.writestr("word/_rels/document.xml.rels", DOCUMENT_RELATIONSHIPS)
        archive.writestr("word/document.xml", document_xml())
        archive.writestr("word/styles.xml", STYLES)
        archive.writestr("word/header1.xml", HEADER)
        archive.writestr("word/footer1.xml", FOOTER)


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit("Usage: create_e2e_docx.py OUTPUT.docx")
    build(Path(sys.argv[1]).resolve())


if __name__ == "__main__":
    main()
