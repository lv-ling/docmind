package com.docmind.api.template.application;

import com.docmind.api.source.domain.SourceFileType;

public interface DocumentPreviewConverter {
  PreviewPdf convert(SourceFileType fileType, byte[] sourceBytes);

  record PreviewPdf(byte[] bytes, Integer pageCount) {}
}
