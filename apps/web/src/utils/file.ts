import {
  MAX_SOURCE_FILE_SIZE_BYTES,
  SOURCE_FILE_TYPES,
  type SourceFileType,
  type SourceMimeType,
} from '@docmind/contracts';

const MIME_BY_EXTENSION: Record<SourceFileType, SourceMimeType> = {
  doc: 'application/msword',
  docx: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  pdf: 'application/pdf',
};

export interface ValidatedSourceFile {
  fileType: SourceFileType;
  mimeType: SourceMimeType;
}

export const validateSourceFile = (file: File): ValidatedSourceFile => {
  const extension = file.name.split('.').pop()?.toLowerCase();
  if (extension === undefined || !SOURCE_FILE_TYPES.includes(extension as SourceFileType)) {
    throw new Error('仅支持 DOC、DOCX、PDF 文件');
  }
  if (file.size === 0) throw new Error('不能上传空文件');
  if (file.size > MAX_SOURCE_FILE_SIZE_BYTES) throw new Error('文件大小不能超过 10 MB');
  const fileType = extension as SourceFileType;
  return { fileType, mimeType: MIME_BY_EXTENSION[fileType] };
};

export const sha256Hex = async (file: File): Promise<string> => {
  const digest = await crypto.subtle.digest('SHA-256', await file.arrayBuffer());
  return Array.from(new Uint8Array(digest), (byte) => byte.toString(16).padStart(2, '0')).join('');
};

export const formatBytes = (value: number): string => {
  if (value < 1024) return `${value} B`;
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`;
  return `${(value / (1024 * 1024)).toFixed(1)} MB`;
};
