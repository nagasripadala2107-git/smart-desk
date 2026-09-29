import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",
  async rewrites() {
    const rawBackend = process.env.INTERNAL_BACKEND_URL;
    const backendUrl = rawBackend
      ? (rawBackend.startsWith("http") ? rawBackend : `http://${rawBackend}`)
      : "http://localhost:8080";

    return [
      {
        source: "/api/v1/:path*",
        destination: `${backendUrl}/api/v1/:path*`,
      },
    ];
  },
};

export default nextConfig;
