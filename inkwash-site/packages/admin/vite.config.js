import { resolve } from 'path';

import { createViteConfig } from '@inkwash/share/vite-config-factory.js';
import tailwindcss from '@tailwindcss/vite';
import vue from '@vitejs/plugin-vue';
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers';
import Components from 'unplugin-vue-components/vite';
import { defineConfig } from 'vite';

export default defineConfig(
  createViteConfig({
    rootDir: import.meta.dirname,
    port: 9090,
    plugins: [
      vue(),
      tailwindcss(),
      Components({
        resolvers: [ElementPlusResolver({ importStyle: false, directives: false })],
        dts: false,
      }),
    ],
    scss: {
      additionalData: `@use "@/assets/styles/variables" as *;\n`,
      loadPaths: [resolve(import.meta.dirname, 'src')],
    },
  }),
);
