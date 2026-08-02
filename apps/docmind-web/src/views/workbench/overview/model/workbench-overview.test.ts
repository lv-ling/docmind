import type { ExtractionSchema, SourceDocumentPage, Template } from '@/contracts';
import { describe, expect, it } from 'vitest';

import { createWorkbenchOverview } from './workbench-overview.js';

const audit = {
  created_at: '2026-08-02T02:00:00Z',
  created_by: 'user-1',
  updated_at: '2026-08-02T03:00:00Z',
  updated_by: 'user-1',
} as const;

describe('createWorkbenchOverview', () => {
  it('retains backend facts while providing frontend-only review and efficiency states', () => {
    const sourcePage = {
      items: [
        {
          ...audit,
          id: 'source-1',
          workspace_id: 'workspace-1',
          name: '合同',
          current_version_id: null,
        },
        {
          ...audit,
          id: 'source-2',
          workspace_id: 'workspace-1',
          name: '发票',
          current_version_id: 'source-version-1',
        },
      ],
      next_cursor: null,
      has_more: true,
    } as SourceDocumentPage;
    const templates = [
      {
        ...audit,
        id: 'template-1',
        workspace_id: 'workspace-1',
        source_document_id: 'source-1',
        source_version_id: 'source-version-1',
        conversion_job_id: 'job-1',
        conversion_status: 'running',
        failure_code: null,
        name: '采购合同模板',
        current_version_id: null,
      },
      {
        ...audit,
        id: 'template-2',
        workspace_id: 'workspace-1',
        source_document_id: 'source-2',
        source_version_id: 'source-version-2',
        conversion_job_id: 'job-2',
        conversion_status: 'failed',
        failure_code: 'CONVERT_FAILED',
        name: '发票模板',
        current_version_id: null,
      },
    ] as Template[];
    const schemas = [
      {
        ...audit,
        id: 'schema-1',
        workspace_id: 'workspace-1',
        name: '合同字段',
        description: '',
        current_version_id: 'schema-version-1',
      },
    ] as ExtractionSchema[];

    const overview = createWorkbenchOverview(sourcePage, templates, schemas);

    expect(overview.backendSummary).toMatchObject({
      documentCount: 2,
      hasMoreDocuments: true,
      activeConversionCount: 1,
      readyTemplateCount: 0,
      publishedSchemaCount: 1,
      pendingSourceCount: 1,
      failedConversionCount: 1,
    });
    expect(overview.activitySummary).toEqual({ processedDocumentCount: 12, attentionCount: 3 });
    expect(overview.attentionItems).toHaveLength(2);
    expect(overview.attentionItems[0]?.extractionFields).toHaveLength(3);
    expect(overview.pipelineItems[0]?.progress).toBe(65);
    expect(overview.efficiency).toMatchObject({
      parsedDocumentCount: 124,
      accuracyRate: 96.5,
      autoArchiveRate: 72,
    });
  });
});
