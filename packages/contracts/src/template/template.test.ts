import { describe, expect, expectTypeOf, it } from 'vitest';

import {
  CONVERSION_WARNING_SEVERITIES,
  TEMPLATE_VERSION_STATUSES,
  type ConversionWarning,
  type CreateTemplateVersionRequest,
  type GeneratedTemplateVersionInput,
  type TemplateResource,
  type TemplateVersion,
} from '../index.js';

describe('template contracts', () => {
  it('publishes version and warning states used by manual validation', () => {
    expect(TEMPLATE_VERSION_STATUSES).toEqual(['generated', 'checking', 'published', 'superseded']);
    expect(CONVERSION_WARNING_SEVERITIES).toEqual(['info', 'warning', 'error']);
  });

  it('keeps template versions tied to source and parsed content', () => {
    expectTypeOf<TemplateVersion>().toHaveProperty('source_version_id');
    expectTypeOf<TemplateVersion>().toHaveProperty('parsed_content_id');
    expectTypeOf<TemplateVersion['document']['html']>().toBeString();
    expectTypeOf<TemplateVersion>().toHaveProperty('created_by');
    expectTypeOf<TemplateResource>().toHaveProperty('download_url');
    expectTypeOf<TemplateResource>().not.toHaveProperty('access_path');
    expectTypeOf<ConversionWarning['blocking']>().toBeBoolean();
    expectTypeOf<CreateTemplateVersionRequest>().toHaveProperty('base_version_id');
    expectTypeOf<CreateTemplateVersionRequest>().not.toHaveProperty('warnings');
    expectTypeOf<GeneratedTemplateVersionInput>().toHaveProperty('warnings');
  });
});
