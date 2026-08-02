<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';

import AppIcon from '@/components/AppIcon.vue';
import { RouteName } from '@/router/constants.js';
import { DmButton } from '@/ui';
import { showToast } from '@/ui/toast.js';

import AiReviewPanel from './components/AiReviewPanel.vue';
import DocumentEvidencePane from './components/DocumentEvidencePane.vue';
import ReviewFieldsPane from './components/ReviewFieldsPane.vue';
import { REVIEW_DOCUMENT, REVIEW_FIELDS } from './model/review-workspace.js';

const router = useRouter();
const activeFieldId = ref('penalty');
const fields = ref(REVIEW_FIELDS.map((field) => ({ ...field })));

const updateField = (fieldId: string, value: string): void => {
  fields.value = fields.value.map((field) => (field.id === fieldId ? { ...field, value } : field));
};

const acceptSuggestion = (fieldId = 'penalty'): void => {
  const field = fields.value.find((item) => item.id === fieldId);
  if (field?.suggestion === undefined) return;
  updateField(fieldId, field.suggestion);
  activeFieldId.value = fieldId;
  showToast('已采纳 AI 建议并修正为 20%');
};

const confirmReview = async (): Promise<void> => {
  showToast('复核已完成，文档已归档');
  await router.push({ name: RouteName.ExtractionReviewQueue });
};
</script>

<template>
  <section class="fixed inset-0 z-50 flex animate-dm-page-fade flex-col bg-zinc-100 text-zinc-900">
    <header
      class="z-20 flex h-12 shrink-0 items-center justify-between border-b border-zinc-200 bg-white px-4 shadow-subtle"
    >
      <div class="flex min-w-0 items-center gap-3">
        <DmButton
          variant="ghost"
          icon-only
          class="shrink-0"
          aria-label="返回审核中心"
          @click="router.push({ name: RouteName.ExtractionReviewQueue })"
        >
          <AppIcon name="arrow-left" class="size-4" />
        </DmButton>
        <span class="h-3 w-px bg-zinc-200"></span>
        <h1
          class="flex min-w-0 items-center gap-2 truncate text-[13px] font-semibold text-zinc-900"
        >
          <AppIcon name="file-text" class="size-3.5 shrink-0 text-zinc-400" />{{
            REVIEW_DOCUMENT.title
          }}
        </h1>
        <span
          class="ml-2 shrink-0 rounded border border-amber-200/50 bg-amber-50 px-2 py-0.5 text-[10px] font-medium text-amber-700"
          >需人工复核</span
        >
      </div>
      <div class="ml-4 flex shrink-0 items-center gap-2">
        <DmButton
          variant="secondary"
          @click="router.push({ name: RouteName.ExtractionReviewQueue })"
          >拒绝并退回</DmButton
        >
        <DmButton variant="dark" @click="confirmReview"
          ><AppIcon name="check" />确认全部并归档</DmButton
        >
      </div>
    </header>
    <div class="flex min-h-0 flex-1 overflow-hidden">
      <DocumentEvidencePane :active-field-id="activeFieldId" />
      <ReviewFieldsPane
        :fields="fields"
        :active-field-id="activeFieldId"
        @select="activeFieldId = $event"
        @update="updateField"
        @accept="acceptSuggestion"
      />
      <AiReviewPanel @accept="acceptSuggestion()" />
    </div>
  </section>
</template>
