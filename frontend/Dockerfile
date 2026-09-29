# ============================================================
# Stage 1: Build Angular
# ============================================================
FROM node:20-alpine AS builder
WORKDIR /app

# Copy package files trước để cache node_modules
COPY package*.json ./
RUN npm ci

# Copy source và build production
COPY . .
RUN npm run build -- --configuration production

# ============================================================
# Stage 2: Nginx serve Angular
# ============================================================
FROM nginx:1.27-alpine

# Copy file build từ stage 1
COPY --from=builder /app/dist/frontend/browser /usr/share/nginx/html

# Copy cấu hình nginx cho Angular (SPA routing)
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
