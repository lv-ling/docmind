import '@/styles/index.css';
import '@/ui/styles.css';

import { createPinia } from 'pinia';
import { createApp } from 'vue';

import App from '@/App.vue';
import { router } from '@/router/index.js';

createApp(App).use(createPinia()).use(router).mount('#app');
