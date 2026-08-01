import type { ApiError } from '@docmind/contracts';

export interface RequestOptions extends Omit<RequestInit, 'body'> {
  body?: unknown;
  idempotencyKey?: string;
}

type UnauthorizedHandler = () => void;
type TokenProvider = () => string | null;

export class ApiClientError extends Error {
  readonly status: number;
  readonly response: ApiError | null;

  constructor(status: number, response: ApiError | null, fallbackMessage: string) {
    super(response?.message ?? fallbackMessage);
    this.name = 'ApiClientError';
    this.status = status;
    this.response = response;
  }
}

let tokenProvider: TokenProvider = () => null;
let unauthorizedHandler: UnauthorizedHandler = () => undefined;

export const configureApiClient = (
  nextTokenProvider: TokenProvider,
  nextUnauthorizedHandler: UnauthorizedHandler,
): void => {
  tokenProvider = nextTokenProvider;
  unauthorizedHandler = nextUnauthorizedHandler;
};

const isApiError = (value: unknown): value is ApiError => {
  if (typeof value !== 'object' || value === null) return false;
  const candidate = value as Partial<ApiError>;
  return typeof candidate.code === 'string' && typeof candidate.message === 'string';
};

const parseResponseBody = async (response: Response): Promise<unknown> => {
  if (response.status === 204) return null;
  const contentType = response.headers.get('content-type') ?? '';
  if (!contentType.includes('application/json')) return response.text();
  return response.json();
};

export const apiRequest = async <T>(path: string, options: RequestOptions = {}): Promise<T> => {
  const { body: requestBody, idempotencyKey, ...requestInit } = options;
  const headers = new Headers(options.headers);
  headers.set('Accept', 'application/json');
  headers.set('X-Request-ID', crypto.randomUUID());

  const token = tokenProvider();
  if (token !== null) headers.set('Authorization', `Bearer ${token}`);
  if (idempotencyKey !== undefined) {
    headers.set('Idempotency-Key', idempotencyKey);
  }
  if (requestBody !== undefined && !(requestBody instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }

  const init: RequestInit = {
    ...requestInit,
    headers,
  };
  if (requestBody !== undefined) {
    init.body = requestBody instanceof FormData ? requestBody : JSON.stringify(requestBody);
  }
  const response = await fetch(path, init);
  const body = await parseResponseBody(response);
  if (!response.ok) {
    if (response.status === 401) unauthorizedHandler();
    throw new ApiClientError(
      response.status,
      isApiError(body) ? body : null,
      `请求失败（HTTP ${response.status}）`,
    );
  }
  return body as T;
};

export const apiRequestBlob = async (path: string): Promise<Blob> => {
  const headers = new Headers({ 'X-Request-ID': crypto.randomUUID() });
  const token = tokenProvider();
  if (token !== null) headers.set('Authorization', `Bearer ${token}`);
  const response = await fetch(path, { headers });
  if (!response.ok) {
    if (response.status === 401) unauthorizedHandler();
    const body = await parseResponseBody(response);
    throw new ApiClientError(
      response.status,
      isApiError(body) ? body : null,
      `文件读取失败（HTTP ${response.status}）`,
    );
  }
  return response.blob();
};

export const getAuthenticatedObjectUrl = async (path: string): Promise<string> =>
  URL.createObjectURL(await apiRequestBlob(path));

export const createIdempotencyKey = (): string => crypto.randomUUID();

export interface UploadProgress {
  loaded: number;
  total: number;
  percentage: number;
}

export interface DirectUploadResult {
  etag: string;
}

export const uploadFileDirectly = (
  url: string,
  file: File,
  requiredHeaders: Record<string, string>,
  onProgress: (progress: UploadProgress) => void,
): Promise<DirectUploadResult> =>
  new Promise((resolve, reject) => {
    const request = new XMLHttpRequest();
    request.open('PUT', url);
    Object.entries(requiredHeaders).forEach(([name, value]) =>
      request.setRequestHeader(name, value),
    );
    request.upload.addEventListener('progress', (event) => {
      const total = event.lengthComputable ? event.total : file.size;
      onProgress({
        loaded: event.loaded,
        total,
        percentage: total === 0 ? 0 : Math.round((event.loaded / total) * 100),
      });
    });
    request.addEventListener('load', () => {
      if (request.status < 200 || request.status >= 300) {
        reject(new Error(`文件上传失败（HTTP ${request.status}）`));
        return;
      }
      resolve({ etag: (request.getResponseHeader('ETag') ?? '').replaceAll('"', '') });
    });
    request.addEventListener('error', () => reject(new Error('文件上传网络连接失败')));
    request.addEventListener('abort', () => reject(new Error('文件上传已取消')));
    request.send(file);
  });
