import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  build: {
    // Some managed Windows environments block Lightning CSS native binaries.
    // The generated CSS remains production-ready; nginx still serves it compressed.
    cssMinify: false,
  },
  server: {
    port: 5173,
    proxy: { '/api': 'http://localhost:8080' },
  },
})
