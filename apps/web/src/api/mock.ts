import type {
  AcceptedExtractionJob,
  AcceptedTemplateJob,
  CompleteSourceUploadRequest,
  CompleteSourceUploadResponse,
  CreateExtractionRequest,
  CreateSchemaRequest,
  CreateSourceUploadRequest,
  CreateSourceUploadResponse,
  CreateTemplateVersionRequest,
  ExtractionFieldResultId,
  ExtractionRunView,
  ExtractionSchema,
  IsoDateTime,
  JsonObject,
  LoginResponse,
  ReviewExtractionFieldRequest,
  SchemaTemplate,
  SchemaVersion,
  SensitiveRuleTemplate,
  SensitiveRuleTemplateVersion,
  SourceDocument,
  SourceDocumentDetail,
  SourceDocumentPage,
  SourcePreviewAccess,
  SourceVersion,
  Template,
  TemplateDetail,
  TemplateVersion,
  UserSummary,
  WorkspaceSummary,
} from '@docmind/contracts';
import {
  createEmptyDocument,
  serializeControlledDocument,
  type ControlledDocument,
  type DocumentNodeId,
} from '@docmind/editor';

import type { DirectUploadResult, RequestOptions, UploadProgress } from './client.js';

const MOCK_WORKSPACE_ID = '10000000-0000-4000-8000-000000000001';
const MOCK_USER_ID = '20000000-0000-4000-8000-000000000001';
const MOCK_SOURCE_ID = '30000000-0000-4000-8000-000000000001';
const MOCK_SOURCE_VERSION_ID = '31000000-0000-4000-8000-000000000001';
const MOCK_SCHEMA_ID = '40000000-0000-4000-8000-000000000001';
const MOCK_SCHEMA_VERSION_ID = '41000000-0000-4000-8000-000000000001';
const MOCK_RULE_TEMPLATE_ID = '50000000-0000-4000-8000-000000000001';
const MOCK_RULE_VERSION_ID = '51000000-0000-4000-8000-000000000001';
const MOCK_EXTRACTION_ID = '60000000-0000-4000-8000-000000000001';
const MOCK_TEMPLATE_ID = '70000000-0000-4000-8000-000000000001';
const MOCK_TEMPLATE_VERSION_ID = '71000000-0000-4000-8000-000000000001';

const now = (daysAgo = 0): IsoDateTime =>
  new Date(Date.now() - daysAgo * 86_400_000).toISOString() as IsoDateTime;
const entityId = <T>(value: string): T => value as unknown as T;
const clone = <T>(value: T): T => structuredClone(value);
const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === 'object' && value !== null && !Array.isArray(value);

export const isMockApiEnabled = (): boolean =>
  import.meta.env.MODE === 'mock' || import.meta.env.VITE_USE_MOCK === 'true';

const mockUser: UserSummary = {
  id: entityId(MOCK_USER_ID),
  email: 'demo@docmind.local',
  display_name: 'DocMind 演示用户',
  status: 'active',
};

const mockWorkspaces: WorkspaceSummary[] = [
  {
    id: entityId(MOCK_WORKSPACE_ID),
    name: '示例文档工作区',
    slug: 'demo-workspace',
    role: 'owner',
    created_at: now(90),
  },
];

const audit = (daysAgo: number) => ({
  created_at: now(daysAgo),
  created_by: entityId<typeof mockUser.id>(MOCK_USER_ID),
  updated_at: now(Math.max(0, daysAgo - 1)),
  updated_by: entityId<typeof mockUser.id>(MOCK_USER_ID),
});

const mockSources: SourceDocument[] = [
  {
    id: entityId(MOCK_SOURCE_ID),
    workspace_id: entityId(MOCK_WORKSPACE_ID),
    name: '华东区采购框架合同',
    current_version_id: entityId(MOCK_SOURCE_VERSION_ID),
    ...audit(3),
  },
  {
    id: entityId('30000000-0000-4000-8000-000000000002'),
    workspace_id: entityId(MOCK_WORKSPACE_ID),
    name: '2026 年供应商准入资料',
    current_version_id: entityId('31000000-0000-4000-8000-000000000002'),
    ...audit(8),
  },
  {
    id: entityId('30000000-0000-4000-8000-000000000003'),
    workspace_id: entityId(MOCK_WORKSPACE_ID),
    name: '产品服务协议（待上传）',
    current_version_id: null,
    ...audit(12),
  },
];

const sourceVersions = new Map<string, SourceVersion>([
  [
    MOCK_SOURCE_VERSION_ID,
    {
      id: entityId(MOCK_SOURCE_VERSION_ID),
      source_document_id: entityId(MOCK_SOURCE_ID),
      workspace_id: entityId(MOCK_WORKSPACE_ID),
      version_number: 2,
      status: 'ready',
      original_file_name: '华东区采购框架合同-v2.pdf',
      file_type: 'pdf',
      declared_mime_type: 'application/pdf',
      expected_size_bytes: 2_486_312,
      file: {
        original_file_name: '华东区采购框架合同-v2.pdf',
        file_type: 'pdf',
        mime_type: 'application/pdf',
        size_bytes: 2_486_312,
        sha256: 'a'.repeat(64),
      },
      failure_code: null,
      created_at: now(3),
    },
  ],
  [
    '31000000-0000-4000-8000-000000000000',
    {
      id: entityId('31000000-0000-4000-8000-000000000000'),
      source_document_id: entityId(MOCK_SOURCE_ID),
      workspace_id: entityId(MOCK_WORKSPACE_ID),
      version_number: 1,
      status: 'ready',
      original_file_name: '华东区采购框架合同-v1.docx',
      file_type: 'docx',
      declared_mime_type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      expected_size_bytes: 1_904_208,
      file: {
        original_file_name: '华东区采购框架合同-v1.docx',
        file_type: 'docx',
        mime_type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        size_bytes: 1_904_208,
        sha256: 'b'.repeat(64),
      },
      failure_code: null,
      created_at: now(21),
    },
  ],
  [
    '31000000-0000-4000-8000-000000000002',
    {
      id: entityId('31000000-0000-4000-8000-000000000002'),
      source_document_id: entityId('30000000-0000-4000-8000-000000000002'),
      workspace_id: entityId(MOCK_WORKSPACE_ID),
      version_number: 1,
      status: 'ready',
      original_file_name: '供应商准入资料.pdf',
      file_type: 'pdf',
      declared_mime_type: 'application/pdf',
      expected_size_bytes: 986_421,
      file: {
        original_file_name: '供应商准入资料.pdf',
        file_type: 'pdf',
        mime_type: 'application/pdf',
        size_bytes: 986_421,
        sha256: 'c'.repeat(64),
      },
      failure_code: null,
      created_at: now(8),
    },
  ],
]);

const mockSchemas: ExtractionSchema[] = [
  {
    id: entityId(MOCK_SCHEMA_ID),
    workspace_id: entityId(MOCK_WORKSPACE_ID),
    name: '采购合同关键字段',
    description: '抽取合同编号、相对方、金额、生效日期和经办人联系方式',
    current_version_id: entityId(MOCK_SCHEMA_VERSION_ID),
    ...audit(15),
  },
  {
    id: entityId('40000000-0000-4000-8000-000000000002'),
    workspace_id: entityId(MOCK_WORKSPACE_ID),
    name: '供应商档案',
    description: '用于供应商准入资料的结构化登记',
    current_version_id: entityId('41000000-0000-4000-8000-000000000002'),
    ...audit(28),
  },
];

const mockSchemaTemplates: SchemaTemplate[] = [
  {
    id: entityId('42000000-0000-4000-8000-000000000001'),
    workspace_id: entityId(MOCK_WORKSPACE_ID),
    name: '采购合同标准模板',
    description: '可复用的采购合同字段集合',
    current_schema_version_id: entityId(MOCK_SCHEMA_VERSION_ID),
    ...audit(14),
  },
];

const mockSensitiveTemplates: SensitiveRuleTemplate[] = [
  {
    id: entityId(MOCK_RULE_TEMPLATE_ID),
    workspace_id: entityId(MOCK_WORKSPACE_ID),
    name: '九国通用敏感信息规则',
    description: '覆盖身份证件、电话号码、邮箱、银行账户和人员姓名',
    current_version_id: entityId(MOCK_RULE_VERSION_ID),
    ...audit(20),
  },
];

let mockExtraction: ExtractionRunView = {
  id: entityId(MOCK_EXTRACTION_ID),
  job_id: entityId('61000000-0000-4000-8000-000000000001'),
  workspace_id: entityId(MOCK_WORKSPACE_ID),
  source_version_id: entityId(MOCK_SOURCE_VERSION_ID),
  schema_version_id: entityId(MOCK_SCHEMA_VERSION_ID),
  sensitive_rule_template_version_id: entityId(MOCK_RULE_VERSION_ID),
  status: 'review_required',
  result: {
    data: {
      contract_number: 'DM-2026-0828',
      supplier_name: '上海澄明数字科技有限公司',
      total_amount: 1280000,
      effective_date: '2026-07-01',
      contact_phone: '138****8899',
    },
    contains_masked_values: true,
    fields: [
      {
        id: entityId('62000000-0000-4000-8000-000000000001'),
        json_path: '$.contract_number',
        display_value: { access: 'visible', value: 'DM-2026-0828' },
        value_source: 'extracted',
        missing_reason: null,
        confidence: 0.98,
        evidence: [
          {
            page_number: 1,
            node_id: 'paragraph-1',
            display_text: '合同编号：DM-2026-0828',
            is_masked: false,
            start_offset: 5,
            end_offset: 17,
          },
        ],
        candidates: [],
        needs_review: false,
        review_status: 'accepted',
      },
      {
        id: entityId('62000000-0000-4000-8000-000000000002'),
        json_path: '$.supplier_name',
        display_value: { access: 'visible', value: '上海澄明数字科技有限公司' },
        value_source: 'extracted',
        missing_reason: null,
        confidence: 0.91,
        evidence: [
          {
            page_number: 1,
            node_id: 'paragraph-3',
            display_text: '乙方：上海澄明数字科技有限公司',
            is_masked: false,
            start_offset: 3,
            end_offset: 16,
          },
        ],
        candidates: [],
        needs_review: true,
        review_status: 'pending',
      },
      {
        id: entityId('62000000-0000-4000-8000-000000000003'),
        json_path: '$.total_amount',
        display_value: { access: 'visible', value: 1280000 },
        value_source: 'extracted',
        missing_reason: null,
        confidence: 0.76,
        evidence: [
          {
            page_number: 2,
            node_id: 'paragraph-8',
            display_text: '合同含税总额为人民币壹佰贰拾捌万元整（¥1,280,000.00）。',
            is_masked: false,
            start_offset: 8,
            end_offset: 25,
          },
        ],
        candidates: [
          {
            display_value: { access: 'visible', value: 1280000 },
            confidence: 0.76,
            evidence: [],
          },
          {
            display_value: { access: 'visible', value: 1200000 },
            confidence: 0.42,
            evidence: [],
          },
        ],
        needs_review: true,
        review_status: 'pending',
      },
      {
        id: entityId('62000000-0000-4000-8000-000000000004'),
        json_path: '$.contact_phone',
        display_value: { access: 'masked', value: null, masked_preview: '138****8899' },
        value_source: 'extracted',
        missing_reason: null,
        confidence: 0.96,
        evidence: [
          {
            page_number: 5,
            node_id: 'paragraph-22',
            display_text: '联系人：张**，电话：138****8899',
            is_masked: true,
            start_offset: 9,
            end_offset: 20,
          },
        ],
        candidates: [],
        needs_review: false,
        review_status: 'accepted',
      },
    ],
    model: {
      provider: 'Mock AI',
      model: 'docmind-demo-1',
      prompt_version: 'mock-v1',
      input_tokens: 4862,
      output_tokens: 318,
    },
    validation_errors: [],
  },
  failure_code: null,
  created_at: now(1),
  completed_at: now(1),
};

const nodeId = (value: string): DocumentNodeId => entityId(value);
const mockDocument: ControlledDocument = createEmptyDocument({
  root_id: nodeId('document-root'),
  title: '华东区采购框架合同',
  language: 'zh-CN',
  template_schema_version_id: entityId(MOCK_SCHEMA_VERSION_ID),
});
mockDocument.metadata.source_page_count = 6;
mockDocument.blocks = [
  {
    id: nodeId('heading-1'),
    type: 'heading',
    level: 1,
    source: { source_node_id: 'source-heading-1', page_number: 1 },
    attributes: {},
    style: { alignment: 'center', spacing_after: { value: 18, unit: 'pt' } },
    content: [
      {
        id: nodeId('text-1'),
        type: 'text',
        text: '采购框架合同',
        source: null,
        attributes: {},
        style: { font_size: { value: 22, unit: 'pt' }, font_weight: 700 },
      },
    ],
  },
  {
    id: nodeId('paragraph-1'),
    type: 'paragraph',
    source: { source_node_id: 'source-paragraph-1', page_number: 1 },
    attributes: {},
    style: { alignment: 'left', line_height: 1.8 },
    content: [
      {
        id: nodeId('text-2'),
        type: 'text',
        text: '合同编号：DM-2026-0828',
        source: null,
        attributes: {},
        style: { font_size: { value: 11, unit: 'pt' } },
      },
    ],
  },
  {
    id: nodeId('paragraph-2'),
    type: 'paragraph',
    source: { source_node_id: 'source-paragraph-2', page_number: 1 },
    attributes: {},
    style: { alignment: 'justify', line_height: 1.8 },
    content: [
      {
        id: nodeId('text-3'),
        type: 'text',
        text: '甲乙双方本着平等互利、诚实信用的原则，就产品采购与服务事项达成本框架协议。',
        source: null,
        attributes: {},
        style: { font_size: { value: 11, unit: 'pt' } },
      },
    ],
  },
  {
    id: nodeId('heading-2'),
    type: 'heading',
    level: 2,
    source: { source_node_id: 'source-heading-2', page_number: 2 },
    attributes: {},
    style: { spacing_before: { value: 16, unit: 'pt' } },
    content: [
      {
        id: nodeId('text-4'),
        type: 'text',
        text: '一、采购范围与合同金额',
        source: null,
        attributes: {},
        style: { font_size: { value: 14, unit: 'pt' }, font_weight: 700 },
      },
    ],
  },
  {
    id: nodeId('paragraph-3'),
    type: 'paragraph',
    source: { source_node_id: 'source-paragraph-8', page_number: 2 },
    attributes: {},
    style: { alignment: 'justify', line_height: 1.8 },
    content: [
      {
        id: nodeId('text-5'),
        type: 'text',
        text: '合同含税总额为人民币壹佰贰拾捌万元整（¥1,280,000.00），具体交付以采购订单为准。',
        source: null,
        attributes: {},
        style: { font_size: { value: 11, unit: 'pt' } },
      },
    ],
  },
];

const serializedDocument = serializeControlledDocument(mockDocument);

const makeTemplateVersion = (
  id: string,
  versionNumber: number,
  status: TemplateVersion['status'],
): TemplateVersion => ({
  id: entityId(id),
  template_id: entityId(MOCK_TEMPLATE_ID),
  workspace_id: entityId(MOCK_WORKSPACE_ID),
  source_version_id: entityId(MOCK_SOURCE_VERSION_ID),
  parsed_content_id: entityId('72000000-0000-4000-8000-000000000001'),
  version_number: versionNumber,
  status,
  document: {
    html: serializedDocument.html,
    css: serializedDocument.css,
    sanitization_policy_version: 'mock-v1',
  },
  document_model: clone(mockDocument) as unknown as JsonObject,
  resources: [],
  warnings: [
    {
      id: entityId('73000000-0000-4000-8000-000000000001'),
      severity: 'warning',
      code: 'FONT_SUBSTITUTED',
      message: '原文档字体已替换为工作台安全字体。',
      source_node_id: 'heading-1',
      page_number: 1,
      fallback: 'Noto Sans CJK SC',
      blocking: false,
    },
  ],
  change_summary: versionNumber === 1 ? '由原始文档生成' : '调整标题与正文间距',
  diff:
    versionNumber === 1
      ? { changes: [] }
      : { changes: [{ kind: 'changed', path: '$.blocks[0].style.spacing_after' }] },
  created_at: now(versionNumber === 1 ? 6 : 2),
  created_by: entityId(MOCK_USER_ID),
  published_at: status === 'published' ? now(2) : null,
});

let templateVersions = [makeTemplateVersion(MOCK_TEMPLATE_VERSION_ID, 1, 'generated')];
const mockTemplates: Template[] = [
  {
    id: entityId(MOCK_TEMPLATE_ID),
    workspace_id: entityId(MOCK_WORKSPACE_ID),
    source_document_id: entityId(MOCK_SOURCE_ID),
    source_version_id: entityId(MOCK_SOURCE_VERSION_ID),
    conversion_job_id: entityId('74000000-0000-4000-8000-000000000001'),
    conversion_status: 'ready',
    failure_code: null,
    name: '采购框架合同标准模板',
    current_version_id: entityId(MOCK_TEMPLATE_VERSION_ID),
    ...audit(6),
  },
];

const sourceDetail = (sourceId: string): SourceDocumentDetail | null => {
  const source = mockSources.find((item) => item.id === sourceId);
  if (source === undefined) return null;
  return {
    source,
    versions: [...sourceVersions.values()].filter(
      (version) => version.source_document_id === source.id,
    ),
  };
};

const previewAccess = (sourceVersionId: string): SourcePreviewAccess => ({
  preview: {
    id: entityId(`preview-${sourceVersionId}`),
    source_version_id: entityId(sourceVersionId),
    status: 'ready',
    format: 'pdf',
    page_count: sourceVersionId === MOCK_SOURCE_VERSION_ID ? 6 : 3,
    failure_code: null,
    created_at: now(1),
    completed_at: now(1),
  },
  view_url: `/api/mock/previews/${sourceVersionId}`,
  original_content_url: `/api/mock/originals/${sourceVersionId}`,
});

const mockSchemaVersion = (request: CreateSchemaRequest, schemaId: string): SchemaVersion => ({
  id: entityId(crypto.randomUUID()),
  schema_id: entityId(schemaId),
  workspace_id: entityId(MOCK_WORKSPACE_ID),
  version_number: 1,
  status: 'published',
  fields: request.fields.map((field) => ({ ...field, id: entityId(crypto.randomUUID()) })),
  json_schema: {},
  change_summary: '由 Mock 工作台创建',
  created_at: now(),
  created_by: entityId(MOCK_USER_ID),
  published_at: now(),
});

const mockRuleVersion = (templateId: string): SensitiveRuleTemplateVersion => ({
  id: entityId(crypto.randomUUID()),
  template_id: entityId(templateId),
  workspace_id: entityId(MOCK_WORKSPACE_ID),
  version_number: 1,
  status: 'published',
  rules: [],
  change_summary: '由 Mock 工作台创建',
  created_at: now(),
  created_by: entityId(MOCK_USER_ID),
  published_at: now(),
});

const createSource = (request: CreateSourceUploadRequest): CreateSourceUploadResponse => {
  const sourceId = crypto.randomUUID();
  const versionId = crypto.randomUUID();
  const source: SourceDocument = {
    id: entityId(sourceId),
    workspace_id: entityId(MOCK_WORKSPACE_ID),
    name: request.document_name,
    current_version_id: null,
    ...audit(0),
  };
  const fileType = request.original_file_name.toLowerCase().endsWith('.pdf')
    ? 'pdf'
    : request.original_file_name.toLowerCase().endsWith('.doc')
      ? 'doc'
      : 'docx';
  const version: SourceVersion = {
    id: entityId(versionId),
    source_document_id: source.id,
    workspace_id: source.workspace_id,
    version_number: 1,
    status: 'uploading',
    original_file_name: request.original_file_name,
    file_type: fileType,
    declared_mime_type: request.declared_mime_type,
    expected_size_bytes: request.size_bytes,
    file: null,
    failure_code: null,
    created_at: now(),
  };
  mockSources.unshift(source);
  sourceVersions.set(versionId, version);
  return {
    source,
    version,
    upload: {
      id: entityId(crypto.randomUUID()),
      source_document_id: source.id,
      source_version_id: version.id,
      status: 'pending',
      upload_url: `mock://upload/${versionId}`,
      upload_method: 'PUT',
      required_headers: {},
      max_size_bytes: 10 * 1024 * 1024,
      expires_at: now(-1),
      created_at: now(),
    },
  };
};

const completeSource = (
  versionId: string,
  request: CompleteSourceUploadRequest,
): CompleteSourceUploadResponse => {
  const version = sourceVersions.get(versionId);
  if (version === undefined) throw new Error('Mock 原件版本不存在');
  version.status = 'ready';
  version.file = {
    original_file_name: version.original_file_name,
    file_type: version.file_type,
    mime_type: request.detected_mime_type,
    size_bytes: request.size_bytes,
    sha256: request.sha256,
  };
  const source = mockSources.find((item) => item.id === version.source_document_id);
  if (source === undefined) throw new Error('Mock 原件不存在');
  source.current_version_id = version.id;
  source.updated_at = now();
  return { source, version };
};

const createMockTemplate = (sourceVersionId: string, name: string): AcceptedTemplateJob => {
  const templateId = crypto.randomUUID();
  const versionId = crypto.randomUUID();
  const sourceVersion = sourceVersions.get(sourceVersionId);
  const sourceId =
    sourceVersion?.source_document_id ?? entityId<Template['source_document_id']>(MOCK_SOURCE_ID);
  const template: Template = {
    id: entityId(templateId),
    workspace_id: entityId(MOCK_WORKSPACE_ID),
    source_document_id: sourceId,
    source_version_id: entityId(sourceVersionId),
    conversion_job_id: entityId(crypto.randomUUID()),
    conversion_status: 'ready',
    failure_code: null,
    name,
    current_version_id: entityId(versionId),
    ...audit(0),
  };
  mockTemplates.unshift(template);
  const version = makeTemplateVersion(versionId, 1, 'generated');
  Object.assign(version, {
    template_id: template.id,
    source_version_id: template.source_version_id,
  });
  templateVersions = [version, ...templateVersions];
  return {
    job_id: template.conversion_job_id,
    template_id: template.id,
    request_id: entityId(crypto.randomUUID()),
  };
};

const getTemplateDetail = (templateId: string): TemplateDetail => {
  const template = mockTemplates.find((item) => item.id === templateId);
  if (template === undefined) throw new Error('Mock 模板不存在');
  const versions = templateVersions.filter((item) => item.template_id === template.id);
  return {
    template,
    current_version:
      versions.find((item) => item.id === template.current_version_id) ?? versions[0] ?? null,
    versions,
  };
};

export const mockApiRequest = async <T>(path: string, options: RequestOptions = {}): Promise<T> => {
  await Promise.resolve();
  const url = new URL(path, 'http://mock.docmind.local');
  const pathname = url.pathname;
  const method = (options.method ?? 'GET').toUpperCase();

  if (method === 'POST' && pathname === '/api/v1/auth/login') {
    const response: LoginResponse = {
      access_token: 'docmind-mock-access-token',
      token_type: 'Bearer',
      expires_in: 86_400,
      user: mockUser,
    };
    return clone(response) as T;
  }
  if (method === 'GET' && pathname === '/api/v1/me') return clone(mockUser) as T;
  if (method === 'GET' && pathname === '/api/v1/workspaces') return clone(mockWorkspaces) as T;

  const workspaceMatch = pathname.match(/^\/api\/v1\/workspaces\/([^/]+)\/(.+)$/);
  if (workspaceMatch !== null) {
    const resource = workspaceMatch[2];
    if (method === 'GET' && resource === 'sources') {
      const page: SourceDocumentPage = {
        items: clone(mockSources),
        next_cursor: null,
        has_more: false,
      };
      return page as T;
    }
    if (method === 'POST' && resource === 'sources' && isRecord(options.body)) {
      return clone(createSource(options.body as unknown as CreateSourceUploadRequest)) as T;
    }
    if (method === 'GET' && resource === 'schemas') return clone(mockSchemas) as T;
    if (method === 'GET' && resource === 'schema-templates') return clone(mockSchemaTemplates) as T;
    if (method === 'GET' && resource === 'sensitive-rule-templates')
      return clone(mockSensitiveTemplates) as T;
    if (method === 'GET' && resource === 'templates') return clone(mockTemplates) as T;
    if (method === 'POST' && resource === 'schemas' && isRecord(options.body)) {
      const request = options.body as unknown as CreateSchemaRequest;
      const schemaId = crypto.randomUUID();
      const currentVersion = mockSchemaVersion(request, schemaId);
      const schema: ExtractionSchema = {
        id: entityId(schemaId),
        workspace_id: entityId(MOCK_WORKSPACE_ID),
        name: request.name,
        description: request.description,
        current_version_id: currentVersion.id,
        ...audit(0),
      };
      mockSchemas.unshift(schema);
      return clone({ schema, current_version: currentVersion, versions: [currentVersion] }) as T;
    }
    if (method === 'POST' && resource === 'sensitive-rule-templates' && isRecord(options.body)) {
      const templateId = crypto.randomUUID();
      const request = options.body;
      const currentVersion = mockRuleVersion(templateId);
      const template: SensitiveRuleTemplate = {
        id: entityId(templateId),
        workspace_id: entityId(MOCK_WORKSPACE_ID),
        name: String(request.name ?? 'Mock 敏感规则'),
        description: String(request.description ?? ''),
        current_version_id: currentVersion.id,
        ...audit(0),
      };
      mockSensitiveTemplates.unshift(template);
      return clone({ template, current_version: currentVersion, versions: [currentVersion] }) as T;
    }
  }

  const sourceMatch = pathname.match(/^\/api\/v1\/sources\/([^/]+)$/);
  if (method === 'GET' && sourceMatch !== null) {
    const detail = sourceDetail(sourceMatch[1] ?? '');
    if (detail === null) throw new Error('Mock 文档不存在');
    return clone(detail) as T;
  }

  const completeMatch = pathname.match(/^\/api\/v1\/source-versions\/([^/]+)\/complete$/);
  if (method === 'POST' && completeMatch !== null && isRecord(options.body)) {
    return clone(
      completeSource(
        completeMatch[1] ?? '',
        options.body as unknown as CompleteSourceUploadRequest,
      ),
    ) as T;
  }

  const previewMatch = pathname.match(/^\/api\/v1\/source-versions\/([^/]+)\/preview$/);
  if (method === 'GET' && previewMatch !== null) {
    return clone(previewAccess(previewMatch[1] ?? MOCK_SOURCE_VERSION_ID)) as T;
  }

  const extractionCreateMatch = pathname.match(
    /^\/api\/v1\/source-versions\/([^/]+)\/extractions$/,
  );
  if (method === 'POST' && extractionCreateMatch !== null) {
    const request = options.body as CreateExtractionRequest;
    mockExtraction = {
      ...clone(mockExtraction),
      id: entityId(crypto.randomUUID()),
      job_id: entityId(crypto.randomUUID()),
      source_version_id: entityId(extractionCreateMatch[1] ?? MOCK_SOURCE_VERSION_ID),
      schema_version_id: request.schema_version_id,
      sensitive_rule_template_version_id: request.sensitive_rule_template_version_id,
      status: 'review_required',
      created_at: now(),
      completed_at: now(),
    };
    const accepted: AcceptedExtractionJob = {
      job_id: mockExtraction.job_id,
      extraction_id: mockExtraction.id,
      request_id: entityId(crypto.randomUUID()),
    };
    return clone(accepted) as T;
  }

  const extractionMatch = pathname.match(/^\/api\/v1\/extractions\/([^/]+)$/);
  if (method === 'GET' && extractionMatch !== null) return clone(mockExtraction) as T;

  const reviewMatch = pathname.match(/^\/api\/v1\/extractions\/([^/]+)\/fields\/([^/]+)$/);
  if (method === 'PATCH' && reviewMatch !== null && isRecord(options.body)) {
    const fieldId = entityId<ExtractionFieldResultId>(reviewMatch[2] ?? '');
    const request = options.body as unknown as ReviewExtractionFieldRequest;
    const field = mockExtraction.result?.fields.find((item) => item.id === fieldId);
    if (field !== undefined) {
      field.review_status =
        request.action === 'accept'
          ? 'accepted'
          : request.action === 'modify'
            ? 'modified'
            : 'rejected';
      field.needs_review = false;
      if (request.action === 'modify') {
        field.display_value = { access: 'visible', value: request.value };
        field.value_source = 'manual';
      }
    }
    return clone(mockExtraction) as T;
  }

  if (method === 'POST' && /^\/api\/v1\/extractions\/[^/]+\/approve$/.test(pathname)) {
    mockExtraction.status = 'approved';
    return clone(mockExtraction) as T;
  }

  const templateCreateMatch = pathname.match(/^\/api\/v1\/source-versions\/([^/]+)\/templates$/);
  if (method === 'POST' && templateCreateMatch !== null && isRecord(options.body)) {
    return clone(
      createMockTemplate(
        templateCreateMatch[1] ?? MOCK_SOURCE_VERSION_ID,
        String(options.body.name ?? '未命名 Mock 模板'),
      ),
    ) as T;
  }

  const templateMatch = pathname.match(/^\/api\/v1\/templates\/([^/]+)$/);
  if (method === 'GET' && templateMatch !== null) {
    return clone(getTemplateDetail(templateMatch[1] ?? '')) as T;
  }

  const createVersionMatch = pathname.match(/^\/api\/v1\/templates\/([^/]+)\/versions$/);
  if (method === 'POST' && createVersionMatch !== null && isRecord(options.body)) {
    const template = mockTemplates.find((item) => item.id === createVersionMatch[1]);
    if (template === undefined) throw new Error('Mock 模板不存在');
    const request = options.body as unknown as CreateTemplateVersionRequest;
    const existing = templateVersions.filter((item) => item.template_id === template.id);
    const version = makeTemplateVersion(
      crypto.randomUUID(),
      Math.max(...existing.map((item) => item.version_number), 0) + 1,
      'generated',
    );
    version.template_id = template.id;
    version.source_version_id = template.source_version_id;
    version.document_model = clone(request.document_model);
    version.change_summary = request.change_summary;
    templateVersions.unshift(version);
    template.current_version_id = version.id;
    template.updated_at = now();
    return clone(version) as T;
  }

  const publishMatch = pathname.match(
    /^\/api\/v1\/templates\/([^/]+)\/versions\/([^/]+)\/publish$/,
  );
  if (method === 'POST' && publishMatch !== null) {
    const version = templateVersions.find((item) => item.id === publishMatch[2]);
    if (version === undefined) throw new Error('Mock 模板版本不存在');
    version.status = 'published';
    version.published_at = now();
    return clone(version) as T;
  }

  const rollbackMatch = pathname.match(/^\/api\/v1\/templates\/([^/]+)\/rollback$/);
  if (method === 'POST' && rollbackMatch !== null && isRecord(options.body)) {
    const body = options.body as Record<string, unknown>;
    const target = templateVersions.find((item) => item.id === body.target_version_id);
    if (target === undefined) throw new Error('Mock 回滚目标不存在');
    const restored = clone(target);
    restored.id = entityId(crypto.randomUUID());
    restored.version_number = Math.max(...templateVersions.map((item) => item.version_number)) + 1;
    restored.status = 'published';
    restored.created_at = now();
    restored.published_at = now();
    templateVersions.unshift(restored);
    const template = mockTemplates.find((item) => item.id === rollbackMatch[1]);
    if (template !== undefined) template.current_version_id = restored.id;
    return clone(restored) as T;
  }

  throw new Error(`尚未实现的 Mock API：${method} ${pathname}`);
};

export const mockApiRequestBlob = async (path: string): Promise<Blob> => {
  await Promise.resolve();
  const title = path.includes('/originals/') ? '不可变原件' : '安全预览';
  const html = `<!doctype html>
<html lang="zh-CN"><head><meta charset="utf-8"><title>${title}</title>
<style>body{margin:0;background:#ddd8cf;font-family:system-ui;color:#17232b}.page{box-sizing:border-box;width:794px;min-height:1123px;margin:24px auto;padding:84px 88px;background:#fff;box-shadow:0 12px 36px #0002}h1{text-align:center;font-size:28px;margin:0 0 42px}p{font-size:16px;line-height:2;text-align:justify}.meta{display:flex;justify-content:space-between;color:#65717a;font-size:13px;border-bottom:1px solid #ccd2d5;padding-bottom:14px;margin-bottom:38px}.stamp{margin-top:80px;text-align:right;color:#9b2d30;font-weight:700}</style></head>
<body><article class="page"><div class="meta"><span>DOCMIND MOCK · ${title}</span><span>第 1 / 6 页</span></div><h1>采购框架合同</h1><p>合同编号：DM-2026-0828</p><p>甲乙双方本着平等互利、诚实信用的原则，就产品采购与服务事项达成本框架协议。</p><p>合同含税总额为人民币壹佰贰拾捌万元整（¥1,280,000.00），具体交付以采购订单为准。</p><p>本页面由纯前端 Mock 数据生成，不依赖 API、数据库、对象存储或文档转换服务。</p><div class="stamp">MOCK DATA</div></article></body></html>`;
  return new Blob([html], { type: 'text/html;charset=utf-8' });
};

export const mockUploadFileDirectly = async (
  file: File,
  onProgress: (progress: UploadProgress) => void,
): Promise<DirectUploadResult> => {
  onProgress({ loaded: file.size, total: file.size, percentage: 100 });
  await Promise.resolve();
  return { etag: `mock-${crypto.randomUUID()}` };
};
