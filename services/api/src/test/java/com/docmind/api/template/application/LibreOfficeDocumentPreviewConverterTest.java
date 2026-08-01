package com.docmind.api.template.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.docmind.api.source.domain.SourceFileType;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;

class LibreOfficeDocumentPreviewConverterTest {

  @Test
  void preservesPdfBytesAndReportsPageCount() throws Exception {
    byte[] pdf;
    try (PDDocument document = new PDDocument();
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      document.addPage(new PDPage());
      document.addPage(new PDPage());
      document.save(output);
      pdf = output.toByteArray();
    }
    LibreOfficeDocumentPreviewConverter converter =
        new LibreOfficeDocumentPreviewConverter("unused", Duration.ofSeconds(1));

    DocumentPreviewConverter.PreviewPdf preview = converter.convert(SourceFileType.PDF, pdf);

    assertThat(preview.bytes()).isEqualTo(pdf);
    assertThat(preview.pageCount()).isEqualTo(2);
  }
}
