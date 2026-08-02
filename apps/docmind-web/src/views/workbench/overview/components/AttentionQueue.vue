<script setup lang="ts">
import AppIcon from '@/components/AppIcon.vue';

import type { WorkbenchAttentionItem } from '../model/workbench-overview.js';

defineProps<{
  items: WorkbenchAttentionItem[];
  attentionCount: number;
}>();

const emit = defineEmits<{
  'open-item': [item: WorkbenchAttentionItem];
}>();
</script>

<template>
  <section id="workbench-attention" class="attention-queue" aria-labelledby="attention-title">
    <header>
      <h2 id="attention-title">需人工干预</h2>
      <span>{{ attentionCount }} 份</span>
    </header>

    <div class="attention-queue__list">
      <article
        v-for="item in items"
        :key="item.id"
        class="attention-card"
        :class="`attention-card--${item.variant}`"
      >
        <span
          v-if="item.variant === 'review'"
          class="attention-card__marker"
          aria-hidden="true"
        ></span>

        <div class="attention-card__heading">
          <span class="attention-card__icon" aria-hidden="true">
            <AppIcon :name="item.variant === 'review' ? 'document' : 'documents'" />
          </span>

          <div class="attention-card__identity">
            <strong>{{ item.title }}</strong>
            <div class="attention-card__metadata">
              <span>{{ item.category }}</span>
              <span class="attention-card__confidence">
                <AppIcon name="review" />{{ item.confidenceLabel }}
              </span>
              <span v-if="item.variant === 'review'">{{ item.updatedAt }}</span>
              <span v-if="item.recommendation" class="attention-card__recommendation">
                <AppIcon name="bot" />{{ item.recommendation }}
              </span>
            </div>
          </div>

          <button type="button" @click="emit('open-item', item)">
            {{ item.actionLabel }}
            <AppIcon v-if="item.variant === 'review'" name="arrow" />
          </button>
        </div>

        <dl v-if="item.extractionFields" class="attention-card__fields">
          <div v-for="field in item.extractionFields" :key="field.label">
            <dt>{{ field.label }}</dt>
            <dd :class="{ 'attention-card__field--mono': field.isMonospace }">
              {{ field.value }}
            </dd>
          </div>
        </dl>

        <div v-if="item.riskNotice" class="attention-card__risk" role="status">
          <AppIcon name="alert" />
          <p>
            <strong>{{ item.riskNotice.title }}</strong>
            {{ item.riskNotice.beforeHighlight }}<mark>{{ item.riskNotice.highlightedValue }}</mark
            >{{ item.riskNotice.afterHighlight }}
          </p>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.attention-queue {
  display: grid;
  gap: 9px;
  scroll-margin-top: 72px;
}

.attention-queue > header {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 22px;
}

.attention-queue h2 {
  margin: 0;
  color: var(--dm-color-zinc-800);
  font-size: 13px;
  font-weight: 700;
}

.attention-queue > header span {
  padding: 2px 7px;
  color: #b45309;
  border-radius: 5px;
  background: #fef3c7;
  font-size: 10px;
  font-weight: 700;
}

.attention-queue__list {
  display: grid;
  gap: 15px;
}

.attention-card {
  position: relative;
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--dm-color-border);
  border-radius: var(--dm-radius-medium);
  background: #fff;
  box-shadow: var(--dm-shadow-card);
  transition:
    border-color var(--dm-motion-fast) var(--dm-motion-easing),
    transform var(--dm-motion-fast) var(--dm-motion-easing),
    box-shadow var(--dm-motion-fast) var(--dm-motion-easing);
}

.attention-card:hover {
  border-color: #c7d2fe;
  box-shadow: 0 5px 16px rgb(99 102 241 / 7%);
  transform: translateY(-1px);
}

.attention-card--review {
  display: grid;
  gap: 11px;
  padding: 18px 15px 16px 18px;
  border-color: #c7d2fe;
}

.attention-card--archive {
  padding: 12px 13px;
}

.attention-card__marker {
  position: absolute;
  inset: 0 auto 0 0;
  width: 3px;
  background: #fbbf24;
}

.attention-card__heading {
  display: grid;
  grid-template-columns: 31px minmax(0, 1fr) auto;
  align-items: center;
  gap: 11px;
}

.attention-card__icon {
  display: grid;
  place-items: center;
  width: 31px;
  height: 31px;
  color: var(--dm-color-zinc-500);
  border: 1px solid var(--dm-color-border);
  border-radius: 6px;
  background: var(--dm-color-zinc-50);
}

.attention-card--review .attention-card__icon {
  color: var(--dm-color-brand);
  border-color: #c7d2fe;
  background: var(--dm-color-brand-soft);
}

.attention-card__icon :deep(.app-icon) {
  width: 15px;
  height: 15px;
}

.attention-card__identity {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.attention-card__identity > strong {
  overflow: hidden;
  color: var(--dm-color-zinc-900);
  font-size: 13px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attention-card--review .attention-card__identity > strong {
  color: var(--dm-color-brand);
}

.attention-card__metadata {
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
  color: var(--dm-color-zinc-500);
  font-size: 11px;
}

.attention-card__metadata > span:first-child,
.attention-card__confidence {
  padding: 2px 6px;
  border-radius: 4px;
  background: var(--dm-color-zinc-100);
}

.attention-card__confidence,
.attention-card__recommendation {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.attention-card__confidence {
  color: var(--dm-color-brand-strong);
  border: 1px solid #e0e7ff;
  background: var(--dm-color-brand-soft);
  font-weight: 650;
}

.attention-card--archive .attention-card__confidence {
  color: #047857;
  border-color: #d1fae5;
  background: #ecfdf5;
}

.attention-card__confidence :deep(.app-icon),
.attention-card__recommendation :deep(.app-icon) {
  width: 12px;
  height: 12px;
}

.attention-card__heading > button {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-height: 30px;
  padding: 6px 10px;
  color: var(--dm-color-zinc-700);
  border: 1px solid var(--dm-color-border);
  border-radius: 5px;
  background: #fff;
  box-shadow: var(--dm-shadow-card);
  font-size: 11px;
  font-weight: 650;
  cursor: pointer;
}

.attention-card--review .attention-card__heading > button {
  color: var(--dm-color-brand);
  border-color: transparent;
  background: var(--dm-color-brand-soft);
  box-shadow: none;
}

.attention-card__heading > button:hover {
  border-color: var(--dm-color-border-strong);
  background: var(--dm-color-zinc-50);
}

.attention-card--review .attention-card__heading > button:hover {
  border-color: #e0e7ff;
  background: #e0e7ff;
}

.attention-card__heading > button :deep(.app-icon) {
  width: 12px;
  height: 12px;
}

.attention-card__fields {
  display: grid;
  grid-template-columns: 1fr 1.55fr 0.8fr;
  gap: 20px;
  padding: 10px 12px;
  margin: 0;
  border: 1px solid var(--dm-color-zinc-100);
  border-radius: 5px;
  background: rgb(250 250 250 / 80%);
}

.attention-card__fields div {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.attention-card__fields dt {
  color: var(--dm-color-zinc-400);
  font-size: 10px;
}

.attention-card__fields dd {
  overflow: hidden;
  margin: 0;
  color: var(--dm-color-zinc-900);
  font-size: 12px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attention-card__field--mono {
  font-family: var(--dm-font-mono);
}

.attention-card__risk {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  color: #92400e;
  border: 1px solid #fef3c7;
  border-radius: 5px;
  background: rgb(255 251 235 / 80%);
}

.attention-card__risk > :deep(.app-icon) {
  width: 14px;
  height: 14px;
  flex: 0 0 auto;
  color: #d97706;
}

.attention-card__risk p {
  margin: 0;
  font-size: 11px;
  line-height: 1.45;
}

.attention-card__risk mark {
  padding: 0 3px;
  color: #92400e;
  border: 1px solid #fde68a;
  border-radius: 4px;
  background: #fff;
  font-family: var(--dm-font-mono);
  font-weight: 700;
}

@media (max-width: 700px) {
  .attention-card__heading {
    grid-template-columns: 31px minmax(0, 1fr);
  }

  .attention-card__heading > button {
    grid-column: 2;
    justify-self: start;
  }

  .attention-card__metadata {
    flex-wrap: wrap;
  }

  .attention-card__fields {
    grid-template-columns: 1fr;
    gap: 8px;
  }
}
</style>
