import '@/ui/styles.css';
import '@/styles/theme.css';
import '@/styles/index.css';
import '@/styles/tailwind.css';

import { createPinia } from 'pinia';
import { createApp } from 'vue';

import App from '@/App.vue';
import { router } from '@/router/index.js';

createApp(App).use(createPinia()).use(router).mount('#app');
