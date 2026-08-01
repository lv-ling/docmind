declare const entityIdBrand: unique symbol;

/** UUID string identifying a persisted DocMind entity. */
export type EntityId<Entity extends string> = string & {
  readonly [entityIdBrand]: Entity;
};

export type WorkspaceId = EntityId<'Workspace'>;
export type WorkspaceMemberId = EntityId<'WorkspaceMember'>;
export type UserId = EntityId<'User'>;
export type RequestId = EntityId<'Request'>;
export type TraceId = EntityId<'Trace'>;
export type JobId = EntityId<'Job'>;
export type SourceDocumentId = EntityId<'SourceDocument'>;
export type SourceVersionId = EntityId<'SourceVersion'>;
export type SourcePreviewId = EntityId<'SourcePreview'>;
export type UploadSessionId = EntityId<'UploadSession'>;
export type SchemaId = EntityId<'Schema'>;
export type SchemaVersionId = EntityId<'SchemaVersion'>;
export type SchemaFieldId = EntityId<'SchemaField'>;
export type SchemaTemplateId = EntityId<'SchemaTemplate'>;
export type SensitiveRuleTemplateId = EntityId<'SensitiveRuleTemplate'>;
export type SensitiveRuleTemplateVersionId = EntityId<'SensitiveRuleTemplateVersion'>;
export type SensitiveRuleId = EntityId<'SensitiveRule'>;
export type SensitiveTokenId = EntityId<'SensitiveToken'>;
export type ExtractionRunId = EntityId<'ExtractionRun'>;
export type ExtractionFieldResultId = EntityId<'ExtractionFieldResult'>;
export type ExtractionEvidenceId = EntityId<'ExtractionEvidence'>;
export type ExtractionCandidateId = EntityId<'ExtractionCandidate'>;
export type TemplateId = EntityId<'Template'>;
export type TemplateVersionId = EntityId<'TemplateVersion'>;
export type ParsedContentId = EntityId<'ParsedContent'>;
export type TemplateResourceId = EntityId<'TemplateResource'>;
export type ConversionWarningId = EntityId<'ConversionWarning'>;
export type DocumentInstanceId = EntityId<'DocumentInstance'>;
export type InstanceVersionId = EntityId<'InstanceVersion'>;
export type EditLockId = EntityId<'EditLock'>;
export type CommentThreadId = EntityId<'CommentThread'>;
export type CommentId = EntityId<'Comment'>;
export type ProofreadingRunId = EntityId<'ProofreadingRun'>;
export type ProofreadingSuggestionId = EntityId<'ProofreadingSuggestion'>;
export type DiffRunId = EntityId<'DiffRun'>;
export type DiffChangeId = EntityId<'DiffChange'>;
export type EventId = EntityId<'Event'>;
