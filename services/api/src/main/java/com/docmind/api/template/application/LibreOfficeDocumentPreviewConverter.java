package com.docmind.api.template.application;

import com.docmind.api.source.domain.SourceFileType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LibreOfficeDocumentPreviewConverter implements DocumentPreviewConverter {
  private static final int MAX_PREVIEW_BYTES = 50 * 1024 * 1024;
  private static final int MAX_PREVIEW_PAGES = 500;
  private final String command;
  private final Duration timeout;

  public LibreOfficeDocumentPreviewConverter(
      @Value("${docmind.template.libreoffice-command:libreoffice}") String command,
      @Value("${docmind.template.preview-timeout:45s}") Duration timeout) {
    this.command = command;
    this.timeout = timeout;
  }

  @Override
  public PreviewPdf convert(SourceFileType fileType, byte[] sourceBytes) {
    if (fileType == SourceFileType.PDF) return validatedPdf(sourceBytes.clone());
    Path work = null;
    try {
      work = Files.createTempDirectory("docmind-preview-");
      Path input = work.resolve("source." + fileType.wireValue());
      Path profile = work.resolve("lo-profile");
      Files.write(input, sourceBytes);
      Files.createDirectory(profile);
      Process process =
          new ProcessBuilder(
                  command,
                  "--headless",
                  "--nologo",
                  "--nolockcheck",
                  "--nodefault",
                  "-env:UserInstallation=" + profile.toUri(),
                  "--convert-to",
                  "pdf",
                  "--outdir",
                  work.toString(),
                  input.toString())
              .redirectErrorStream(true)
              .redirectOutput(ProcessBuilder.Redirect.DISCARD)
              .start();
      if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
        process.destroyForcibly();
        throw new TemplateConversionException("PREVIEW_CONVERSION_TIMEOUT", true);
      }
      Path output = work.resolve("source.pdf");
      if (process.exitValue() != 0 || !Files.isRegularFile(output)) {
        throw new TemplateConversionException("PREVIEW_CONVERSION_FAILED", true);
      }
      long size = Files.size(output);
      if (size <= 0 || size > MAX_PREVIEW_BYTES) {
        throw new TemplateConversionException("PREVIEW_OUTPUT_INVALID", false);
      }
      return validatedPdf(Files.readAllBytes(output));
    } catch (TemplateConversionException exception) {
      throw exception;
    } catch (IOException exception) {
      throw new TemplateConversionException("LIBREOFFICE_UNAVAILABLE", true, exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new TemplateConversionException("PREVIEW_CONVERSION_INTERRUPTED", true, exception);
    } finally {
      deleteTree(work);
    }
  }

  private PreviewPdf validatedPdf(byte[] bytes) {
    try (PDDocument document = Loader.loadPDF(bytes)) {
      int pageCount = document.getNumberOfPages();
      if (pageCount <= 0 || pageCount > MAX_PREVIEW_PAGES) {
        throw new TemplateConversionException("PREVIEW_OUTPUT_INVALID", false);
      }
      return new PreviewPdf(bytes, pageCount);
    } catch (TemplateConversionException exception) {
      throw exception;
    } catch (IOException exception) {
      throw new TemplateConversionException("PREVIEW_OUTPUT_INVALID", false, exception);
    }
  }

  private void deleteTree(Path root) {
    if (root == null) return;
    try (var paths = Files.walk(root)) {
      paths.sorted(Comparator.reverseOrder()).forEach(this::deleteQuietly);
    } catch (IOException ignored) {
      // Temporary conversion files are best-effort cleanup only.
    }
  }

  private void deleteQuietly(Path path) {
    try {
      Files.deleteIfExists(path);
    } catch (IOException ignored) {
      // Temporary conversion files are best-effort cleanup only.
    }
  }
}
