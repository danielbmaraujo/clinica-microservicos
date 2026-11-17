import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Frontend na porta 3000 (é a origem liberada no CORS dos 3 serviços)
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    strictPort: true, // se a 3000 estiver ocupada, dá erro em vez de usar outra porta
  },
});
