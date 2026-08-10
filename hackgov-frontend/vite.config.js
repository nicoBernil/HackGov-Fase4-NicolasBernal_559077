import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Configuracao do Vite com suporte ao React.
// O servidor de desenvolvimento sobe em http://localhost:5173
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173
  }
})
