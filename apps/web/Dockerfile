# --- build stage: install deps + produce the standalone Next.js build ---
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
# API_URL is baked into next.config rewrites at build time; compose overrides it to the
# in-network api host. Default keeps a host-run build working.
ARG API_URL=http://localhost:8081
ENV API_URL=$API_URL
RUN npm run build

# --- run stage: minimal node image running the standalone server ---
FROM node:22-alpine AS run
WORKDIR /app
ENV NODE_ENV=production
ENV PORT=3000
ENV HOSTNAME=0.0.0.0
# API_URL is also read at RUNTIME by the server-side status fetch (lib/api.ts).
ARG API_URL=http://localhost:8081
ENV API_URL=$API_URL
COPY --from=build /app/public ./public
COPY --from=build /app/.next/standalone ./
COPY --from=build /app/.next/static ./.next/static
EXPOSE 3000
CMD ["node", "server.js"]
