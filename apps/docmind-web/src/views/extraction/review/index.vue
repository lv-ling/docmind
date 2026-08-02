<script setup lang="ts">
import { DmButton, DmSplitPane, DmStatus } from '@/ui';

import InlineNotice from '@/components/InlineNotice.vue';
import { useExtractionReview } from './composables/useExtractionReview.js';
import {
  getConfidenceLabel,
  getExtractionDisplayValueText,
  getExtractionStatusLabel,
  getExtractionStatusTone,
  getExtractionValueBadge,
} from './model/review-display.js';

const {
  extractionId,
  run,
  previewObjectUrl,
  isLoadingReview,
  reviewError,
  connectionNotice,
  isApprovingExtraction,
  savingFieldId,
  editingFieldId,
  editedValue,
  splitPercentage,
  isLeftPanelCollapsed,
  isRightPanelCollapsed,
  fields,
  selectedField,
  reviewProgress,
  canApprove,
  submitFieldReview,
  beginFieldModification,
  approveExtractionResult,
  selectField,
} = useExtractionReview();
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
        <DmStatus
          :label="getExtractionStatusLabel(run.status)"
          :tone="getExtractionStatusTone(run.status)"
          live
        />
        <DmButton
          :disabled="!canApprove"
          :loading="isApprovingExtraction"
          @click="approveExtractionResult"
          >批准抽取结果</DmButton
        >
      </div>
    </header>
    <InlineNotice v-if="reviewError" tone="danger" title="操作未完成" :detail="reviewError" />
    <InlineNotice v-if="connectionNotice" tone="info" title="连接状态" :detail="connectionNotice" />
    <div v-if="isLoadingReview" class="document-loading">正在读取抽取任务…</div>
    <template v-else-if="run">
      <section v-if="!run.result" class="processing-ledger">
        <div class="processing-orbit" aria-hidden="true"><i></i><span>AI</span></div>
        <p class="eyebrow">PROCESSING PIPELINE</p>
        <h2>{{ getExtractionStatusLabel(run.status) }}</h2>
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
          v-model="splitPercentage"
          v-model:left-collapsed="isLeftPanelCollapsed"
          v-model:right-collapsed="isRightPanelCollapsed"
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
                :aria-label="`选择字段 ${field.json_path} 查看证据`"
                tabindex="0"
                @click="selectField(field.id)"
                @focus="selectField(field.id)"
                @keydown.enter="selectField(field.id)"
                @keydown.space.prevent="selectField(field.id)"
              >
                <div class="field-review-head">
                  <code>{{ field.json_path }}</code
                  ><span>{{ getExtractionValueBadge(field) }}</span>
                </div>
                <pre :class="{ masked: field.display_value.access === 'masked' }">{{
                  getExtractionDisplayValueText(field.display_value)
                }}</pre>
                <div class="confidence-line">
                  <span>置信度</span
                  ><i><b :style="{ width: `${(field.confidence ?? 0) * 100}%` }"></b></i
                  ><strong>{{ getConfidenceLabel(field.confidence) }}</strong>
                </div>
                <details v-if="field.candidates.length > 1">
                  <summary>{{ field.candidates.length }} 个候选值</summary>
                  <ol>
                    <li v-for="(candidate, index) in field.candidates" :key="index">
                      <span>{{ getExtractionDisplayValueText(candidate.display_value) }}</span
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
                      @click="submitFieldReview(field, 'modify')"
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
                      @click="submitFieldReview(field, 'reject')"
                      >拒绝</DmButton
                    ><DmButton
                      size="small"
                      variant="secondary"
                      @click="beginFieldModification(field)"
                      >修改</DmButton
                    ><DmButton
                      size="small"
                      :loading="savingFieldId === field.id"
                      @click="submitFieldReview(field, 'accept')"
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

<style src="./styles.css"></style>
