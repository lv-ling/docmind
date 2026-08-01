package com.docmind.api.source.application;

import com.docmind.api.shared.error.ApiErrorCategory;
import com.docmind.api.shared.error.ApiErrorCode;
import com.docmind.api.shared.error.ApiException;
import com.docmind.api.source.domain.SourceFileType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class SourceFileValidator {

  public static final long MAX_FILE_SIZE_BYTES = 10_485_760;
  private static final long MAX_DOCX_EXPANDED_BYTES = 100L * 1024 * 1024;
  private static final int MAX_DOCX_ENTRIES = 10_000;
  private static final byte[] OLE_HEADER =
      {(byte) 0xd0, (byte) 0xcf, 0x11, (byte) 0xe0, (byte) 0xa1, (byte) 0xb1, 0x1a, (byte) 0xe1};
  private static final byte[] PDF_HEADER = "%PDF-".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] PDF_EOF = "%%EOF".getBytes(StandardCharsets.US_ASCII);
  private static final Set<String> DOCX_REQUIRED_ENTRIES =
      Set.of("[Content_Types].xml", "word/document.xml");

  public ValidatedSourceFile validate(byte[] bytes, SourceFileType expectedType) {
    if (bytes.length == 0) {
      throw validationError(ApiErrorCode.VALIDATION_FAILED, "文件不能为空");
    }
    if (bytes.length > MAX_FILE_SIZE_BYTES) {
      throw new ApiException(
          HttpStatus.PAYLOAD_TOO_LARGE,
          ApiErrorCode.FILE_TOO_LARGE,
          ApiErrorCategory.VALIDATION,
          "文件不能超过 10 MiB");
    }

    SourceFileType detected = detect(bytes);
    if (detected != expectedType) {
      throw validationError(ApiErrorCode.FILE_TYPE_NOT_ALLOWED, "文件内容与扩展名不匹配");
    }
    return new ValidatedSourceFile(detected, detected.mimeType(), bytes.length, sha256(bytes));
  }

  private SourceFileType detect(byte[] bytes) {
    if (looksLikePdf(bytes)) {
      return SourceFileType.PDF;
    }
    if (startsWith(bytes, OLE_HEADER)) {
      validateWordOle(bytes);
      return SourceFileType.DOC;
    }
    if (looksLikeZip(bytes)) {
      validateDocx(bytes);
      return SourceFileType.DOCX;
    }
    throw validationError(ApiErrorCode.FILE_TYPE_NOT_ALLOWED, "仅支持有效的 DOC、DOCX 或 PDF 文件");
  }

  private boolean looksLikePdf(byte[] bytes) {
    int headerLimit = Math.min(bytes.length - PDF_HEADER.length, 1024);
    boolean headerFound = false;
    for (int offset = 0; offset <= headerLimit; offset++) {
      if (startsWithAt(bytes, PDF_HEADER, offset)) {
        headerFound = true;
        break;
      }
    }
    if (!headerFound) {
      return false;
    }
    int eofStart = Math.max(0, bytes.length - 4096);
    for (int offset = bytes.length - PDF_EOF.length; offset >= eofStart; offset--) {
      if (startsWithAt(bytes, PDF_EOF, offset)) {
        return true;
      }
    }
    throw validationError(ApiErrorCode.FILE_TYPE_NOT_ALLOWED, "PDF 文件结构不完整");
  }

  private void validateWordOle(byte[] bytes) {
    try (POIFSFileSystem fileSystem =
        new POIFSFileSystem(new ByteArrayInputStream(bytes))) {
      if (!fileSystem.getRoot().hasEntry("WordDocument")) {
        throw validationError(ApiErrorCode.FILE_TYPE_NOT_ALLOWED, "OLE 文件不是有效的 Word 文档");
      }
    } catch (ApiException exception) {
      throw exception;
    } catch (IOException | RuntimeException exception) {
      throw validationError(ApiErrorCode.FILE_TYPE_NOT_ALLOWED, "DOC 文件结构无效");
    }
  }

  private void validateDocx(byte[] bytes) {
    java.util.HashSet<String> found = new java.util.HashSet<>();
    long expandedBytes = 0;
    int entries = 0;
    byte[] buffer = new byte[8192];
    try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null) {
        entries++;
        if (entries > MAX_DOCX_ENTRIES) {
          throw validationError(ApiErrorCode.FILE_TYPE_NOT_ALLOWED, "DOCX 条目数量异常");
        }
        String name = entry.getName();
        if (name.startsWith("/") || name.contains("../") || name.contains("\\")) {
          throw validationError(ApiErrorCode.FILE_TYPE_NOT_ALLOWED, "DOCX 包含不安全的条目路径");
        }
        if (name.equalsIgnoreCase("word/vbaProject.bin")) {
          throw validationError(ApiErrorCode.FILE_TYPE_NOT_ALLOWED, "DOCX 不允许包含宏代码");
        }
        if (DOCX_REQUIRED_ENTRIES.contains(name)) {
          found.add(name);
        }
        int read;
        while ((read = zip.read(buffer)) != -1) {
          expandedBytes += read;
          if (expandedBytes > MAX_DOCX_EXPANDED_BYTES) {
            throw validationError(ApiErrorCode.FILE_TYPE_NOT_ALLOWED, "DOCX 解压后大小异常");
          }
        }
        zip.closeEntry();
      }
    } catch (ApiException exception) {
      throw exception;
    } catch (IOException exception) {
      throw validationError(ApiErrorCode.FILE_TYPE_NOT_ALLOWED, "DOCX 文件结构无效");
    }
    if (!found.containsAll(DOCX_REQUIRED_ENTRIES)) {
      throw validationError(ApiErrorCode.FILE_TYPE_NOT_ALLOWED, "ZIP 文件不是有效的 DOCX 文档");
    }
  }

  private boolean looksLikeZip(byte[] bytes) {
    return bytes.length >= 4
        && bytes[0] == 'P'
        && bytes[1] == 'K'
        && ((bytes[2] == 3 && bytes[3] == 4)
            || (bytes[2] == 5 && bytes[3] == 6)
            || (bytes[2] == 7 && bytes[3] == 8));
  }

  private boolean startsWith(byte[] source, byte[] prefix) {
    return startsWithAt(source, prefix, 0);
  }

  private boolean startsWithAt(byte[] source, byte[] prefix, int offset) {
    if (offset < 0 || source.length - offset < prefix.length) {
      return false;
    }
    for (int index = 0; index < prefix.length; index++) {
      if (source[offset + index] != prefix[index]) {
        return false;
      }
    }
    return true;
  }

  private String sha256(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }

  private ApiException validationError(ApiErrorCode code, String message) {
    return new ApiException(HttpStatus.BAD_REQUEST, code, ApiErrorCategory.VALIDATION, message);
  }

  public record ValidatedSourceFile(
      SourceFileType fileType, String mimeType, long sizeBytes, String sha256) {}
}
