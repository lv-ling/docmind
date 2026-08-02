import type { SourceDocument } from '@/contracts';

export type SourceFilter = 'all' | 'registered' | 'pending';

export const SOURCE_PAGE_SIZE = 6;

export const filterSources = (
  sources: SourceDocument[],
  searchQuery: string,
  sourceFilter: SourceFilter,
): SourceDocument[] => {
  const query = searchQuery.trim().toLocaleLowerCase('zh-CN');
  return sources.filter((source) => {
    const matchesQuery =
      query.length === 0 || source.name.toLocaleLowerCase('zh-CN').includes(query);
    const matchesFilter =
      sourceFilter === 'all' ||
      (sourceFilter === 'registered' && source.current_version_id !== null) ||
      (sourceFilter === 'pending' && source.current_version_id === null);
    return matchesQuery && matchesFilter;
  });
};

export const formatSourceDate = (value: string): string =>
  new Date(value).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  });

export const getSourceVersionLabel = (source: SourceDocument): string =>
  source.current_version_id?.slice(0, 8) ?? '待上传';
