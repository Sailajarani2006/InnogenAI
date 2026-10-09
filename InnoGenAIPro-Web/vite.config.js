import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api/github/token': {
        target: 'https://github.com/login/oauth/access_token',
        changeOrigin: true,
        rewrite: () => '',
        headers: {
          'Accept': 'application/json'
        }
      },
      '/api/github/device': {
        target: 'https://github.com/login/device/code',
        changeOrigin: true,
        rewrite: () => '',
        headers: {
          'Accept': 'application/json'
        }
      }
    }
  }
})
