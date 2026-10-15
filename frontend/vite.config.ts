import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // 개발 중에는 /api 요청(SSE 포함)을 Spring Boot 서버로 넘긴다. CORS 설정 없이 같은 출처처럼 동작
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
