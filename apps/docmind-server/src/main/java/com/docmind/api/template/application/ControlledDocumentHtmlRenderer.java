package com.docmind.api.template.application;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ControlledDocumentHtmlRenderer {
  public static final String POLICY_VERSION = "docmind-controlled-html/v1";
  public static final String CONTROLLED_CSS =
      ".dm-document{box-sizing:border-box;margin:0 auto;background:#fff;}"
          + ".dm-document *{box-sizing:border-box;}"
          + ".dm-document table{border-collapse:collapse;max-width:100%;width:100%;}"
          + ".dm-document td,.dm-document th{border:1px solid #aaa;padding:4pt;}"
          + ".dm-document img{max-width:100%;}"
          + ".dm-page-break{break-after:page;height:0;}"
          + ".dm-page-marker{border-top:1px dashed #bbb;margin:12pt 0;}"
          + ".dm-placeholder{border-radius:2px;background:#fff3bf;color:#664d03;}"
          + ".dm-repeat{border:1px dashed #8c8c8c;}";
  private static final Pattern SAFE_COLOR =
      Pattern.compile("^(?:#[0-9a-fA-F]{3,8}|[a-zA-Z]{3,20})$");
  private static final Pattern SAFE_FONT =
      Pattern.compile("^[\\p{L}\\p{N} _.,'\\-]{1,200}$");

  private final ControlledDocumentValidator validator;

  public ControlledDocumentHtmlRenderer(ControlledDocumentValidator validator) {
    this.validator = validator;
  }

  public RenderedDocument render(JsonNode document) {
    validator.validate(document);
    StringBuilder html = new StringBuilder();
    JsonNode layout = document.path("page_layout");
    html.append("<article class=\"dm-document\" data-dm-document-version=\"1.0\" data-dm-root-id=\"")
        .append(attr(document.path("root_id").asText()))
        .append("\" lang=\"")
        .append(attr(document.path("metadata").path("language").asText("und")))
        .append("\"")
        .append(style(java.util.Arrays.asList(
            declaration("width", length(layout.path("width"))),
            declaration("min-height", length(layout.path("height"))),
            declaration("padding", margins(layout.path("margins"))))))
        .append(">");
    html.append("<template data-dm-document-model=\"1.0\">")
        .append(text(document.toString()))
        .append("</template>");
    renderRegions(html, document.path("headers"), "header");
    html.append("<main>");
    renderBlocks(html, document.path("blocks"));
    html.append("</main>");
    renderRegions(html, document.path("footers"), "footer");
    html.append("</article>");
    return new RenderedDocument(html.toString(), CONTROLLED_CSS, POLICY_VERSION);
  }

  private void renderRegions(StringBuilder html, JsonNode regions, String tag) {
    for (JsonNode region : regions) {
      html.append('<').append(tag).append(" data-dm-region-variant=\"")
          .append(attr(region.path("variant").asText()))
          .append("\">");
      renderBlocks(html, region.path("blocks"));
      html.append("</").append(tag).append('>');
    }
  }

  private void renderBlocks(StringBuilder html, JsonNode blocks) {
    for (JsonNode block : blocks) renderBlock(html, block);
  }

  private void renderBlock(StringBuilder html, JsonNode node) {
    String type = node.path("type").asText();
    String attributes = nodeAttributes(node);
    switch (type) {
      case "paragraph" -> {
        html.append("<p").append(attributes).append(paragraphStyle(node.path("style"))).append('>');
        renderInline(html, node.path("content"));
        html.append("</p>");
      }
      case "heading" -> {
        int level = Math.max(1, Math.min(6, node.path("level").asInt(1)));
        html.append("<h").append(level).append(attributes).append(paragraphStyle(node.path("style"))).append('>');
        renderInline(html, node.path("content"));
        html.append("</h").append(level).append('>');
      }
      case "list" -> {
        String tag = node.path("ordered").asBoolean() ? "ol" : "ul";
        html.append('<').append(tag).append(attributes).append('>');
        for (JsonNode item : node.path("items")) {
          html.append("<li").append(nodeAttributes(item)).append('>');
          renderBlocks(html, item.path("blocks"));
          html.append("</li>");
        }
        html.append("</").append(tag).append('>');
      }
      case "table" -> {
        html.append("<table").append(attributes).append("><tbody>");
        for (JsonNode row : node.path("rows")) {
          html.append("<tr").append(nodeAttributes(row)).append('>');
          for (JsonNode cell : row.path("cells")) {
            String tag = row.path("is_header").asBoolean() ? "th" : "td";
            html.append('<').append(tag).append(nodeAttributes(cell));
            int rowSpan = cell.path("row_span").asInt(1);
            int colSpan = cell.path("column_span").asInt(1);
            if (rowSpan > 1) html.append(" rowspan=\"").append(rowSpan).append('"');
            if (colSpan > 1) html.append(" colspan=\"").append(colSpan).append('"');
            html.append('>');
            renderBlocks(html, cell.path("blocks"));
            html.append("</").append(tag).append('>');
          }
          html.append("</tr>");
        }
        html.append("</tbody></table>");
      }
      case "image" -> html.append("<img").append(attributes)
          .append(" src=\"/api/v1/template-resources/")
          .append(attr(node.path("resource_id").asText()))
          .append("/content\" alt=\"")
          .append(attr(node.path("alt_text").asText()))
          .append("\"")
          .append(style(java.util.Arrays.asList(
              declaration("width", length(node.path("width"))),
              declaration("height", length(node.path("height"))))))
          .append('>');
      case "table_of_contents" -> html.append("<nav").append(attributes)
          .append(" aria-label=\"").append(attr(node.path("title").asText())).append("\"><p>")
          .append(text(node.path("title").asText())).append("</p></nav>");
      case "page_break" -> html.append("<div").append(attributes).append(" class=\"dm-page-break\" role=\"separator\"></div>");
      case "page_marker" -> html.append("<div").append(attributes).append(" class=\"dm-page-marker\" data-dm-page-number=\"")
          .append(node.path("page_number").asInt()).append("\"></div>");
      case "template_repeat" -> {
        html.append("<section").append(attributes).append(" class=\"dm-repeat\">");
        renderBlocks(html, node.path("blocks"));
        html.append("</section>");
      }
      default -> throw new TemplateConversionException("CONTROLLED_DOCUMENT_INVALID", false);
    }
  }

  private void renderInline(StringBuilder html, JsonNode content) {
    for (JsonNode node : content) {
      String type = node.path("type").asText();
      String attributes = nodeAttributes(node);
      switch (type) {
        case "text" -> html.append("<span").append(attributes).append(textStyle(node.path("style"))).append('>')
            .append(text(node.path("text").asText())).append("</span>");
        case "line_break" -> html.append("<br").append(attributes).append('>');
        case "tab" -> html.append("<span").append(attributes).append(">\t</span>");
        case "dynamic_field" -> html.append("<span").append(attributes).append(" data-dm-dynamic-field=\"")
            .append(attr(node.path("field").asText())).append("\">")
            .append('{').append(text(node.path("field").asText().toUpperCase(Locale.ROOT))).append("}</span>");
        case "template_placeholder" -> html.append("<span").append(attributes).append(" class=\"dm-placeholder\" data-dm-json-path=\"")
            .append(attr(node.path("binding").path("json_path").asText())).append("\">{{")
            .append(text(node.path("label").asText())).append("}}</span>");
        default -> throw new TemplateConversionException("CONTROLLED_DOCUMENT_INVALID", false);
      }
    }
  }

  private String nodeAttributes(JsonNode node) {
    return " data-dm-node-id=\"" + attr(node.path("id").asText())
        + "\" data-dm-node-type=\"" + attr(node.path("type").asText("container")) + "\"";
  }

  private String textStyle(JsonNode style) {
    List<String> declarations = new ArrayList<>();
    String font = style.path("font_family").asText("");
    if (SAFE_FONT.matcher(font).matches()) declarations.add("font-family:" + font);
    add(declarations, "font-size", length(style.path("font_size")));
    if (style.path("font_weight").canConvertToInt()) {
      int weight = style.path("font_weight").asInt();
      if (weight >= 1 && weight <= 1000) declarations.add("font-weight:" + weight);
    }
    if (style.path("italic").isBoolean()) {
      declarations.add("font-style:" + (style.path("italic").asBoolean() ? "italic" : "normal"));
    }
    addColor(declarations, "color", style.path("color").asText(""));
    addColor(declarations, "background-color", style.path("background_color").asText(""));
    return style(declarations);
  }

  private String paragraphStyle(JsonNode style) {
    List<String> declarations = new ArrayList<>();
    String alignment = style.path("alignment").asText("");
    if (List.of("left", "center", "right", "justify").contains(alignment)) {
      declarations.add("text-align:" + alignment);
    }
    if (style.path("line_height").isNumber()) {
      double value = style.path("line_height").asDouble();
      if (value > 0 && value <= 10) declarations.add("line-height:" + value);
    }
    add(declarations, "margin-top", length(style.path("spacing_before")));
    add(declarations, "margin-bottom", length(style.path("spacing_after")));
    add(declarations, "text-indent", length(style.path("first_line_indent")));
    add(declarations, "margin-left", length(style.path("left_indent")));
    add(declarations, "margin-right", length(style.path("right_indent")));
    if (style.path("page_break_before").asBoolean(false)) declarations.add("break-before:page");
    return style(declarations);
  }

  private String margins(JsonNode margins) {
    if (!margins.isObject()) return null;
    String top = length(margins.path("top"));
    String right = length(margins.path("right"));
    String bottom = length(margins.path("bottom"));
    String left = length(margins.path("left"));
    if (top == null || right == null || bottom == null || left == null) return null;
    return top + " " + right + " " + bottom + " " + left;
  }

  private String length(JsonNode value) {
    if (!value.isObject() || !value.path("value").isNumber()) return null;
    double number = value.path("value").asDouble();
    String unit = value.path("unit").asText();
    if (!Double.isFinite(number)
        || number < -10000
        || number > 10000
        || !List.of("pt", "px", "mm", "cm", "in", "percent").contains(unit)) return null;
    return number + ("percent".equals(unit) ? "%" : unit);
  }

  private String declaration(String name, String value) {
    return value == null ? null : name + ":" + value;
  }

  private void add(List<String> values, String name, String value) {
    if (value != null) values.add(name + ":" + value);
  }

  private void addColor(List<String> values, String name, String value) {
    if (SAFE_COLOR.matcher(value).matches()) values.add(name + ":" + value);
  }

  private String style(List<String> declarations) {
    List<String> safe = declarations.stream().filter(java.util.Objects::nonNull).toList();
    return safe.isEmpty() ? "" : " style=\"" + attr(String.join(";", safe) + ";") + "\"";
  }

  private String text(String value) {
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }

  private String attr(String value) {
    return text(value).replace("\"", "&quot;").replace("'", "&#39;");
  }

  public record RenderedDocument(String html, String css, String policyVersion) {}
}
