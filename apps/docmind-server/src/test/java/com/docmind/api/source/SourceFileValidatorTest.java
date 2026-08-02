package com.docmind.api.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.docmind.api.shared.error.ApiErrorCode;
import com.docmind.api.shared.error.ApiException;
import com.docmind.api.source.application.SourceFileValidator;
import com.docmind.api.source.domain.SourceFileType;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.junit.jupiter.api.Test;

class SourceFileValidatorTest {

  private final SourceFileValidator validator = new SourceFileValidator();

  @Test
  void detectsPdfDocxAndLegacyWordFromTheirServerSideStructure() throws Exception {
    byte[] pdf = "%PDF-1.7\n1 0 obj\n<<>>\nendobj\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
    byte[] docx = docx(false);
    byte[] doc = legacyDoc();

    assertThat(validator.validate(pdf, SourceFileType.PDF).mimeType())
        .isEqualTo("application/pdf");
    assertThat(validator.validate(docx, SourceFileType.DOCX).mimeType())
        .isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    assertThat(validator.validate(doc, SourceFileType.DOC).mimeType())
        .isEqualTo("application/msword");
  }

  @Test
  void rejectsDisguisedFilesAndMacroEnabledPackages() throws Exception {
    byte[] pdf = "%PDF-1.7\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);

    assertThatThrownBy(() -> validator.validate(pdf, SourceFileType.DOCX))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception -> assertThat(exception.code()).isEqualTo(ApiErrorCode.FILE_TYPE_NOT_ALLOWED));
    assertThatThrownBy(() -> validator.validate(docx(true), SourceFileType.DOCX))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception -> assertThat(exception.code()).isEqualTo(ApiErrorCode.FILE_TYPE_NOT_ALLOWED));
  }

  private byte[] docx(boolean macro) throws Exception {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(output)) {
      addZipEntry(zip, "[Content_Types].xml", "<Types/>");
      addZipEntry(zip, "word/document.xml", "<w:document/>");
      if (macro) {
        addZipEntry(zip, "word/vbaProject.bin", "macro");
      }
    }
    return output.toByteArray();
  }

  private void addZipEntry(ZipOutputStream zip, String name, String value) throws Exception {
    zip.putNextEntry(new ZipEntry(name));
    zip.write(value.getBytes(StandardCharsets.UTF_8));
    zip.closeEntry();
  }

  private byte[] legacyDoc() throws Exception {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (POIFSFileSystem fileSystem = new POIFSFileSystem()) {
      fileSystem
          .getRoot()
          .createDocument(
              "WordDocument", new java.io.ByteArrayInputStream(new byte[] {1, 2, 3, 4}));
      fileSystem.writeFilesystem(output);
    }
    return output.toByteArray();
  }
}
