<template>
  <section class="extraction-route-boundary" aria-labelledby="processing-page-title">
    <h1 id="processing-page-title">AI 处理中心</h1>
  </section>
</template>

<style scoped>
.extraction-route-boundary {
  width: min(100%, var(--dm-layout-content-max));
  min-height: 100%;
  padding: var(--dm-space-6);
  margin: 0 auto;
  color: var(--dm-color-zinc-900);
  background: var(--dm-color-paper);
  animation: route-boundary-reveal var(--dm-motion-page) var(--dm-motion-easing) both;
}

.extraction-route-boundary h1 {
  margin: 0;
  font-size: 18px;
  font-weight: 650;
}

@keyframes route-boundary-reveal {
  from {
    opacity: 0;
    transform: translateY(2px);
  }
}

@media (prefers-reduced-motion: reduce) {
  .extraction-route-boundary {
    animation: none;
  }
}
</style>
