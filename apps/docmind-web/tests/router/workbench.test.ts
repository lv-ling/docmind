import { describe, expect, it } from 'vitest';

import { RouteName } from '@/router/constants.js';
import { workbenchRoute } from '@/router/modules/workbench.js';

const findChildRoute = (routeName: string) =>
  workbenchRoute.children?.find((route) => route.name === routeName);

describe('workbench route boundaries', () => {
  it('keeps processing, review queue, and review workspace as separate routes', () => {
    const processingRoute = findChildRoute(RouteName.ExtractionProcessing);
    const queueRoute = findChildRoute(RouteName.ExtractionReviewQueue);
    const reviewRoute = findChildRoute(RouteName.ExtractionReview);

    expect(processingRoute?.path).toBe('extraction/processing');
    expect(queueRoute?.path).toBe('extraction/queue');
    expect(reviewRoute?.path).toBe('extraction/review');
    expect(reviewRoute?.meta?.hidden).toBe(true);
    expect(reviewRoute?.meta?.menuKey).toBe(RouteName.ExtractionReviewQueue);
    expect(reviewRoute?.meta?.requiredQuery).toEqual(['extractionId']);
  });

  it('preserves the existing authenticated workspace route boundary', () => {
    expect(workbenchRoute.meta).toMatchObject({
      requiresAuth: true,
      requiresWorkspace: true,
    });
    expect(findChildRoute(RouteName.SourceList)).toBeDefined();
    expect(findChildRoute(RouteName.SchemaList)).toBeDefined();
    expect(findChildRoute(RouteName.TemplateList)).toBeDefined();
  });
});
