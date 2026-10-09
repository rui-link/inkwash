import { resolve } from 'path';

import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vite';

export default defineConfig({
  plugins: [vue()],
  build: {
    esbuild: false,
    minify: 'rolldown',
    lib: {
      entry: resolve(import.meta.dirname, 'src/index.js'),
      name: 'InkWashShare',
      fileName: 'inkwash-share',
    },
    rollupOptions: {
      external: [
        'vue',
        'vue-router',
        'pinia',
        'vue-i18n',
        'element-plus',
        '@vueuse/core',
        'axios',
        'markdown-it',
        'md-editor-v3',
        'nprogress',
      ],
      output: {
        globals: {
          vue: 'Vue',
          'vue-router': 'VueRouter',
          pinia: 'Pinia',
          'vue-i18n': 'VueI18n',
          'element-plus': 'ElementPlus',
          '@vueuse/core': 'VueUse',
          axios: 'axios',
          'markdown-it': 'markdownit',
          'md-editor-v3': 'MdEditorV3',
          nprogress: 'NProgress',
        },
      },
    },
  },
});
