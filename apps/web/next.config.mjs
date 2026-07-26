/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  // Lean production image: bundle a minimal standalone server (used by the Docker run stage).
  output: "standalone",
  async rewrites() {
    // Proxy API calls to the Spring Boot management API during local dev.
    return [
      {
        source: "/api/:path*",
        destination: `${process.env.API_URL ?? "http://localhost:8081"}/:path*`,
      },
    ];
  },
};

export default nextConfig;
