<script setup lang="ts">
import type {
  ExtractionDisplayValue,
  ExtractionFieldResultView,
  ExtractionRunId,
  ExtractionRunView,
  ExtractionRunStatus,
  JsonValue,
  SourcePreviewAccess,
} from '@/contracts';
import { DmButton, DmSplitPane, DmStatus } from '@/ui';
import { computed, onMounted, onUnmounted, ref, shallowRef } from 'vue';
import { useRoute } from 'vue-router';

import { getAuthenticatedObjectUrl } from '../api/client.js';
import {
  approveExtraction,
  connectExtractionEvents,
  getExtraction,
  reviewExtractionField,
} from '../api/extractions.js';
import { getSourcePreview } from '../api/sources.js';
import InlineNotice from '../components/InlineNotice.vue';
import { useAuthStore } from '../stores/auth.js';

const route = useRoute();
const auth = useAuthStore();
const extractionId = computed(() => route.params.extractionId as ExtractionRunId);
const run = shallowRef<ExtractionRunView | null>(null);
const preview = shallowRef<SourcePreviewAccess | null>(null);
const previewObjectUrl = ref<string | null>(null);
const loading = ref(true);
const error = ref('');
const info = ref('');
const approving = ref(false);
const savingFieldId = ref<string | null>(null);
const editingFieldId = ref<string | null>(null);
const editedValue = ref('');
const selectedFieldId = ref<string | null>(null);
const split = ref(43);
const leftCollapsed = ref(false);
const rightCollapsed = ref(false);
let stopEvents: (() => void) | null = null;
let pollTimer: ReturnType<typeof setTimeout> | null = null;
let previewTimer: ReturnType<typeof setTimeout> | null = null;

const terminalStatuses: ExtractionRunStatus[] = ['review_required', 'approved', 'failed'];
const fields = computed<ExtractionFieldResultView[]>(() => run.value?.result?.fields ?? []);
const selectedField = computed<ExtractionFieldResultView | null>(
  () => fields.value.find((field) => field.id === selectedFieldId.value) ?? fields.value[0] ?? null,
);
const reviewProgress = computed(() => {
  if (fields.value.length === 0) return 0;
  return Math.round(
    (fields.value.filter((field) => field.review_status !== 'pending').length /
      fields.value.length) *
      100,
  );
});
const canApprove = computed(
  () =>
    run.value?.status === 'review_required' &&
    fields.value.every((field) => field.review_status !== 'pending'),
);

const statusLabel = (status: ExtractionRunStatus): string =>
  ({
    queued: '排队中',
    running: '识别中',
    retrying: '正在重试',
    review_required: '待人工复核',
    approved: '已批准',
    failed: '处理失败',
  })[status];
const statusTone = (status: ExtractionRunStatus): 'info' | 'success' | 'warning' | 'danger' => {
  if (status === 'approved') return 'success';
  if (status === 'failed') return 'danger';
  if (status === 'review_required') return 'warning';
  return 'info';
};

const displayValueText = (display: ExtractionDisplayValue): string => {
  if (display.access === 'masked') return display.masked_preview;
  if (display.value === null) return 'null';
  if (display.value === '') return '空字符串 ""';
  if (typeof display.value === 'string') return display.value;
  return JSON.stringify(display.value, null, 2);
};

const valueBadge = (field: ExtractionFieldResultView): string => {
  if (field.display_value.access === 'masked') return '已脱敏';
  if (field.display_value.value === null)
    return field.missing_reason === null ? 'NULL' : `缺失 · ${field.missing_reason}`;
  if (field.display_value.value === '') return '空字符串';
  return field.value_source === 'default' ? '默认值' : field.value_source;
};

const confidenceLabel = (confidence: number | null | undefined): string =>
  confidence == null || !Number.isFinite(confidence) ? '—' : `${Math.round(confidence * 100)}%`;

const loadPreview = async (): Promise<void> => {
  if (run.value === null) return;
  try {
    preview.value = await getSourcePreview(run.value.source_version_id);
    if (
      preview.value.preview.status === 'ready' &&
      preview.value.view_url !== null &&
      previewObjectUrl.value === null
    ) {
      previewObjectUrl.value = await getAuthenticatedObjectUrl(preview.value.view_url);
    } else if (['queued', 'processing'].includes(preview.value.preview.status)) {
      previewTimer = setTimeout(loadPreview, 2500);
    }
  } catch {
    preview.value = null;
  }
};

const load = async (silent = false): Promise<void> => {
  if (!silent) loading.value = true;
  try {
    run.value = await getExtraction(extractionId.value);
    if (selectedFieldId.value === null)
      selectedFieldId.value = run.value.result?.fields[0]?.id ?? null;
    if (preview.value === null) await loadPreview();
  } catch (caught) {
    error.value = caught instanceof Error ? caught.message : '抽取结果加载失败';
  } finally {
    loading.value = false;
  }
};

const startPolling = (): void => {
  if (pollTimer !== null) return;
  const poll = async (): Promise<void> => {
    await load(true);
    if (run.value !== null && !terminalStatuses.includes(run.value.status)) {
      pollTimer = setTimeout(poll, 2200);
    } else {
      pollTimer = null;
    }
  };
  pollTimer = setTimeout(poll, 500);
};

const startEvents = (): void => {
  if (auth.accessToken === null) return;
  stopEvents = connectExtractionEvents(
    extractionId.value,
    auth.accessToken,
    () => void load(true),
    () => {
      info.value = '实时通道暂不可用，已自动切换为状态轮询。';
      startPolling();
    },
  );
};

const submitReview = async (
  field: ExtractionFieldResultView,
  action: 'accept' | 'modify' | 'reject',
): Promise<void> => {
  savingFieldId.value = field.id;
  error.value = '';
  try {
    let value: JsonValue | null = null;
    if (action === 'modify') {
      try {
        value = JSON.parse(editedValue.value) as JsonValue;
      } catch {
        value = editedValue.value;
      }
    }
    run.value = await reviewExtractionField(extractionId.value, field.id, {
      action,
      value,
      reason: action === 'reject' ? '复核人确认该字段不应采用' : null,
    });
    editingFieldId.value = null;
  } catch (caught) {
    error.value = caught instanceof Error ? caught.message : '字段复核保存失败';
  } finally {
    savingFieldId.value = null;
  }
};

const beginModify = (field: ExtractionFieldResultView): void => {
  editingFieldId.value = field.id;
  editedValue.value =
    field.display_value.access === 'visible' && field.display_value.value !== null
      ? typeof field.display_value.value === 'string'
        ? field.display_value.value
        : JSON.stringify(field.display_value.value)
      : '';
};

const approve = async (): Promise<void> => {
  approving.value = true;
  try {
    run.value = await approveExtraction(extractionId.value, { note: 'Web 工作台复核完成' });
  } catch (caught) {
    error.value = caught instanceof Error ? caught.message : '批准失败';
  } finally {
    approving.value = false;
  }
};

onMounted(async () => {
  await load();
  if (run.value !== null && !terminalStatuses.includes(run.value.status)) startEvents();
});
onUnmounted(() => {
  stopEvents?.();
  if (pollTimer !== null) clearTimeout(pollTimer);
  if (previewTimer !== null) clearTimeout(previewTimer);
  if (previewObjectUrl.value !== null) URL.revokeObjectURL(previewObjectUrl.value);
});
</script>

<template>
  <section class="page-stack review-page">
    <header class="page-heading page-heading--actions">
      <div>
        <p class="eyebrow">HUMAN REVIEW / W3</p>
        <h1>抽取复核</h1>
        <p class="mono-copy">RUN {{ extractionId }}</p>
      </div>
      <div class="review-header-actions" v-if="run">
        <DmStatus :label="statusLabel(run.status)" :tone="statusTone(run.status)" live />
        <DmButton :disabled="!canApprove" :loading="approving" @click="approve"
          >批准抽取结果</DmButton
        >
      </div>
    </header>
    <InlineNotice v-if="error" tone="danger" title="操作未完成" :detail="error" />
    <InlineNotice v-if="info" tone="info" title="连接状态" :detail="info" />
    <div v-if="loading" class="document-loading">正在读取抽取任务…</div>
    <template v-else-if="run">
      <section v-if="!run.result" class="processing-ledger">
        <div class="processing-orbit" aria-hidden="true"><i></i><span>AI</span></div>
        <p class="eyebrow">PROCESSING PIPELINE</p>
        <h2>{{ statusLabel(run.status) }}</h2>
        <ol>
          <li class="done">原件哈希复核</li>
          <li :class="{ active: run.status !== 'failed' }">解析、去标识化与结构化抽取</li>
          <li>结果二次 PII 扫描</li>
          <li>等待人工复核</li>
        </ol>
        <InlineNotice
          v-if="run.status === 'failed' && run.failure_code"
          tone="danger"
          title="抽取任务失败"
          :detail="`失败代码：${run.failure_code}`"
        />
      </section>
      <template v-else>
        <div class="review-summary">
          <div>
            <span>复核进度</span><strong>{{ reviewProgress }}%</strong
            ><i><b :style="{ width: `${reviewProgress}%` }"></b></i>
          </div>
          <dl>
            <div>
              <dt>模型</dt>
              <dd>{{ run.result.model.provider }} / {{ run.result.model.model }}</dd>
            </div>
            <div>
              <dt>字段</dt>
              <dd>{{ fields.length }}</dd>
            </div>
            <div>
              <dt>需重点复核</dt>
              <dd>{{ fields.filter((field) => field.needs_review).length }}</dd>
            </div>
            <div>
              <dt>权限掩码</dt>
              <dd>{{ run.result.contains_masked_values ? '已应用' : '当前角色可见' }}</dd>
            </div>
          </dl>
        </div>
        <DmSplitPane
          v-model="split"
          v-model:left-collapsed="leftCollapsed"
          v-model:right-collapsed="rightCollapsed"
          left-label="原始文档"
          right-label="字段复核"
          class="review-split"
        >
          <template #left>
            <div class="review-original">
              <header>
                <p class="eyebrow">SOURCE EVIDENCE</p>
                <strong>不可变原件</strong>
              </header>
              <iframe v-if="previewObjectUrl" :src="previewObjectUrl" title="抽取原始文档"></iframe>
              <div v-else class="preview-placeholder">
                <strong>预览尚未就绪</strong><span>仍可依据右侧证据摘录完成复核。</span>
              </div>
              <aside v-if="selectedField">
                <p>当前字段证据</p>
                <blockquote
                  v-for="(evidence, index) in selectedField.evidence"
                  :key="`${evidence.node_id}-${index}`"
                >
                  <span>P{{ evidence.page_number ?? '—' }} · {{ evidence.node_id }}</span
                  >{{ evidence.display_text }}
                </blockquote>
                <p v-if="selectedField.evidence.length === 0" class="muted-copy">
                  模型未返回直接证据。
                </p>
              </aside>
            </div>
          </template>
          <template #right>
            <div class="review-fields">
              <header>
                <div>
                  <p class="eyebrow">FIELD DECISIONS</p>
                  <strong>逐字段确认</strong>
                </div>
                <span
                  >{{ fields.filter((field) => field.review_status !== 'pending').length }} /
                  {{ fields.length }}</span
                >
              </header>
              <article
                v-for="field in fields"
                :key="field.id"
                :class="{ selected: selectedField?.id === field.id, attention: field.needs_review }"
                @click="selectedFieldId = field.id"
              >
                <div class="field-review-head">
                  <code>{{ field.json_path }}</code
                  ><span>{{ valueBadge(field) }}</span>
                </div>
                <pre :class="{ masked: field.display_value.access === 'masked' }">{{
                  displayValueText(field.display_value)
                }}</pre>
                <div class="confidence-line">
                  <span>置信度</span
                  ><i><b :style="{ width: `${(field.confidence ?? 0) * 100}%` }"></b></i
                  ><strong>{{ confidenceLabel(field.confidence) }}</strong>
                </div>
                <details v-if="field.candidates.length > 1">
                  <summary>{{ field.candidates.length }} 个候选值</summary>
                  <ol>
                    <li v-for="(candidate, index) in field.candidates" :key="index">
                      <span>{{ displayValueText(candidate.display_value) }}</span
                      ><strong>{{ Math.round(candidate.confidence * 100) }}%</strong>
                    </li>
                  </ol>
                </details>
                <div v-if="editingFieldId === field.id" class="modify-field" @click.stop>
                  <label>修正值<textarea v-model="editedValue" rows="3"></textarea></label>
                  <div>
                    <DmButton size="small" variant="secondary" @click="editingFieldId = null"
                      >取消</DmButton
                    ><DmButton
                      size="small"
                      :loading="savingFieldId === field.id"
                      @click="submitReview(field, 'modify')"
                      >保存修正</DmButton
                    >
                  </div>
                </div>
                <footer v-else @click.stop>
                  <DmStatus
                    v-if="field.review_status !== 'pending'"
                    :label="field.review_status"
                    :tone="field.review_status === 'rejected' ? 'danger' : 'success'"
                  />
                  <template v-else
                    ><DmButton
                      size="small"
                      variant="ghost"
                      :loading="savingFieldId === field.id"
                      @click="submitReview(field, 'reject')"
                      >拒绝</DmButton
                    ><DmButton size="small" variant="secondary" @click="beginModify(field)"
                      >修改</DmButton
                    ><DmButton
                      size="small"
                      :loading="savingFieldId === field.id"
                      @click="submitReview(field, 'accept')"
                      >接受</DmButton
                    ></template
                  >
                </footer>
              </article>
            </div>
          </template>
        </DmSplitPane>
      </template>
    </template>
  </section>
</template>
