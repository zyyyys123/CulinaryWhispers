import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import path from 'path';
// https://vitejs.dev/config/
export default defineConfig({
    plugins: [vue()],
    resolve: {
        alias: {
            '@': path.resolve(__dirname, './src'),
        },
    },
    server: {
        port: 5173,
        open: true,
        proxy: {
            '/api': {
                target: process.env.VITE_API_TARGET || 'http://localhost:8080',
                changeOrigin: true,
            },
        },
    }
});
