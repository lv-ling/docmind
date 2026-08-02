package com.docmind.api.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.docmind.api.template.application.ControlledDocumentHtmlRenderer;
import com.docmind.api.template.application.ControlledDocumentValidator;
import com.docmind.api.template.application.TemplateConversionException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ControlledDocumentHtmlRendererTest {
  private ObjectMapper objectMapper;
  private ControlledDocumentHtmlRenderer renderer;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    renderer =
        new ControlledDocumentHtmlRenderer(new ControlledDocumentValidator(objectMapper));
  }

  @Test
  void rendersOnlyEscapedTextAndAllowlistedStyles() throws Exception {
    JsonNode document = validDocument();
    ((ObjectNode) document.at("/blocks/0/content/0"))
        .put("text", "<script>alert(1)</script>");
    ((ObjectNode) document.at("/blocks/0/content/0/style"))
        .put("font_family", "Arial;position:fixed")
        .put("color", "#123456");

    var result = renderer.render(document);

    assertThat(result.policyVersion()).isEqualTo("docmind-controlled-html/v1");
    assertThat(result.html()).doesNotContain("<script>");
    assertThat(result.html().substring(result.html().indexOf("</template>")))
        .doesNotContain("position:fixed", "font-family:");
    assertThat(result.html()).contains("&lt;script&gt;alert(1)&lt;/script&gt;", "color:#123456");
  }

  @Test
  void rejectsUnknownNodesAndDuplicateStableIds() throws Exception {
    JsonNode unknown = validDocument();
    ((ObjectNode) unknown.at("/blocks/0")).put("type", "script");
    assertThatThrownBy(() -> renderer.render(unknown))
        .isInstanceOf(TemplateConversionException.class)
        .extracting(error -> ((TemplateConversionException) error).failureCode())
        .isEqualTo("CONTROLLED_DOCUMENT_INVALID");

    JsonNode duplicate = validDocument();
    ((ObjectNode) duplicate.at("/blocks/0/content/0")).put("id", "paragraph-1");
    assertThatThrownBy(() -> renderer.render(duplicate))
        .isInstanceOf(TemplateConversionException.class);
  }

  private JsonNode validDocument() throws Exception {
    return objectMapper.readTree(
        """
        {
          "model_version": "1.0",
          "root_id": "document-root",
          "template_schema_version_id": null,
          "metadata": {"title": "安全模板", "language": "zh-CN", "source_page_count": 1},
          "page_layout": {
            "size": "a4",
            "orientation": "portrait",
            "width": {"value": 210, "unit": "mm"},
            "height": {"value": 297, "unit": "mm"},
            "margins": {
              "top": {"value": 25.4, "unit": "mm"},
              "right": {"value": 25.4, "unit": "mm"},
              "bottom": {"value": 25.4, "unit": "mm"},
              "left": {"value": 25.4, "unit": "mm"}
            },
            "header_distance": {"value": 12.7, "unit": "mm"},
            "footer_distance": {"value": 12.7, "unit": "mm"}
          },
          "headers": [],
          "footers": [],
          "blocks": [{
            "id": "paragraph-1",
            "type": "paragraph",
            "source": {"source_node_id": "p1", "page_number": 1},
            "attributes": {},
            "style": {"alignment": "left"},
            "content": [{
              "id": "text-1",
              "type": "text",
              "source": {"source_node_id": "r1", "page_number": 1},
              "attributes": {},
              "text": "正文",
              "style": {}
            }]
          }]
        }
        """);
  }
}
