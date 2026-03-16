/// <reference types="vitest/config" />
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import TanStackRouterVite from "@tanstack/router-plugin/vite";

// https://vite.dev/config/
export default defineConfig({
    plugins: [TanStackRouterVite({ target: "react", autoCodeSplitting: true }), react()],
    build: {
        outDir: "build/dist/static",
    },
    server: {
        proxy: {
            "/api": "http://localhost:8080",
        },
    },
    resolve: {
        alias: {
            // https://github.com/tabler/tabler-icons/issues/1233
            "@tabler/icons-react": "@tabler/icons-react/dist/esm/icons/index.mjs",
        },
    },
    test: {
        environment: "jsdom",
        setupFiles: "vitest.setup.ts",
    },
});
