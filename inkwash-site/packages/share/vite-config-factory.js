import { resolve } from 'path';

export function createViteConfig(options = {}) {
  const { rootDir, scss = null, external = [], port } = options;

  const apiUrl = process.env.VITE_API_URL || 'http://localhost:9080';

  return {
    plugins: options.plugins || [],
    publicDir: resolve(rootDir, '../../public'),
    resolve: {
      alias: {
        '@': resolve(rootDir, 'src'),
      },
    },
    server: {
      host: '0.0.0.0',
      port,
      proxy: {
        '/api': {
          target: apiUrl,
          changeOrigin: true,
        },
        '/uploads': {
          target: apiUrl,
          changeOrigin: true,
        },
        '/oauth2': {
          target: apiUrl,
          changeOrigin: false,
        },
        '/.well-known': {
          target: apiUrl,
          changeOrigin: false,
        },
      },
    },
    ...(scss
      ? {
          css: {
            preprocessorOptions: {
              scss: {
                api: 'modern-compiler',
                additionalData: scss.additionalData || '',
                loadPaths: scss.loadPaths || [],
              },
            },
          },
        }
      : {}),
    build: {
      esbuild: false,
      rollup: {
        rolldown: true,
        moduleStrategy: 'esm',
      },
      outDir: 'dist',
      assetsDir: 'assets',
      emptyOutDir: true,
      minify: 'rolldown',
      cssMinify: 'rolldown',
      cssCodeSplit: true,
      sourcemap: false,
      reportCompressedSize: false,
      chunkSizeWarningLimit: 2048,
      rollupOptions: {
        external: [/^@inkwash\/share/, ...external],
        output: {
          entryFileNames: 'scripts/[name]-[hash].js',
          chunkFileNames: 'scripts/[name]-[hash].js',
          assetFileNames: (asset) => {
            const name = asset.name || '';
            if (/\.(png|jpe?g|gif|svg|webp|ico)$/i.test(name)) {
              return 'assets/images/[name]-[hash][extname]';
            }
            if (/\.(woff2?|eot|ttf|otf)$/i.test(name)) {
              return 'assets/fonts/[name]-[hash][extname]';
            }
            if (/\.(css|scss|sass)$/i.test(name)) {
              return 'styles/[name]-[hash].css';
            }
            return 'assets/[name]-[hash][extname]';
          },
          manualChunks: (id) => {
            if (id.includes('node_modules')) {
              if (
                id.includes('vue') ||
                id.includes('@vue') ||
                id.includes('pinia') ||
                id.includes('vue-router') ||
                id.includes('vue-i18n')
              ) {
                return 'vendor-vue';
              }
              if (id.includes('element-plus')) {
                return 'vendor-ui';
              }
              if (id.includes('echarts')) {
                return 'vendor-chart';
              }
              return 'vendor';
            }
          },
        },
      },
    },
  };
}
