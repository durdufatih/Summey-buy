import type { NextConfig } from "next";

// STATIC_EXPORT=1 npm run build  →  "out/" klasörüne statik site (GitHub Pages, Netlify vb.)
const nextConfig: NextConfig = {
  output: process.env.STATIC_EXPORT ? "export" : undefined,
  basePath: process.env.BASE_PATH || undefined,
  images: { unoptimized: true },
};

export default nextConfig;
