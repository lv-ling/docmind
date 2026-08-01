import { describe, expectTypeOf, it } from 'vitest';

import type { components, operations, paths } from '@docmind/contracts/openapi';

describe('generated OpenAPI types', () => {
  it('exposes typed paths, schemas, and operations from the package subpath', () => {
    expectTypeOf<paths>().toHaveProperty('/source-versions/{sourceVersionId}/extractions');
    expectTypeOf<paths>().toHaveProperty('/source-versions/{sourceVersionId}/complete');
    expectTypeOf<paths>().toHaveProperty('/source-versions/{sourceVersionId}/content');
    expectTypeOf<paths>().toHaveProperty('/source-versions/{sourceVersionId}/preview');
    expectTypeOf<paths>().toHaveProperty('/schemas/{schemaId}');
    expectTypeOf<paths>().toHaveProperty(
      '/sensitive-rule-templates/{sensitiveRuleTemplateId}/versions',
    );
    expectTypeOf<paths>().toHaveProperty('/auth/login');
    expectTypeOf<components['schemas']>().toHaveProperty('SafeHtmlDocument');
    expectTypeOf<components['schemas']>().toHaveProperty('WorkspaceMember');
    expectTypeOf<components['schemas']>().toHaveProperty('SourceDocumentDetail');
    expectTypeOf<components['schemas']>().toHaveProperty('ExtractionSchemaDetail');
    expectTypeOf<components['schemas']>().toHaveProperty('SensitiveRuleTemplateDetail');
    expectTypeOf<components['schemas']>().toHaveProperty('AcceptedExtractionJob');
    expectTypeOf<components['schemas']>().toHaveProperty('ExtractionRunView');
    expectTypeOf<operations>().toHaveProperty('createDiffRun');
    expectTypeOf<operations>().toHaveProperty('login');
    expectTypeOf<operations>().toHaveProperty('createSensitiveRuleTemplateVersion');
    expectTypeOf<operations>().toHaveProperty('createExtractionRun');
    expectTypeOf<operations>().toHaveProperty('getExtractionRun');
  });
});
