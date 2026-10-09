import { fileURLToPath } from 'node:url';

import vue from '@vitejs/plugin-vue';
import { defineConfig, configDefaults } from 'vitest/config';

const r = (p) => fileURLToPath(new URL(p, import.meta.url));

export default defineConfig({
  configLoader: 'bundled',
  test: {
    environment: 'jsdom',
    exclude: [...configDefaults.exclude, 'e2e/**'],
    root: fileURLToPath(new URL('./', import.meta.url)),
    projects: [
      {
        extends: true,
        test: { name: 'share', include: ['packages/share/tests/**/*.spec.js'] },
      },
      {
        extends: true,
        test: { name: 'admin', include: ['packages/admin/tests/**/*.spec.js'] },
        plugins: [vue()],
        resolve: { alias: { '@': r('./packages/admin/src') } },
      },
      {
        extends: true,
        test: { name: 'web', include: ['packages/web/tests/**/*.spec.js'] },
        plugins: [vue()],
        resolve: { alias: { '@': r('./packages/web/src') } },
      },
    ],
  },
});
