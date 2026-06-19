import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const remoteTarget = env.VITE_PROXY_TARGET || 'http://localhost:8080'
  const backendTarget = env.VITE_BACKEND_TARGET || 'http://localhost:8081'

  return {
    plugins: [react()],
    server: {
      proxy: {
        '/api/v1/payment-batches': {
          target: backendTarget,
          changeOrigin: true,
        },
        '/api': {
          target: remoteTarget,
          changeOrigin: true,
        },
      },
    },
  }
})
