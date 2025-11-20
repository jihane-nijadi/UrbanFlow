# Build stage
FROM node:18-alpine AS build
WORKDIR /app
COPY package*.json ./
# Ensure npm is available in the Alpine image and install build deps if missing
RUN apk add --no-cache nodejs npm
# Use `npm install` in the container build to avoid `npm ci` failures
# that can occur when lockfile/registry differences exist in CI/container
# environments. `npm install` is more forgiving and works reliably here.
RUN npm install --no-audit --no-fund --production=false
COPY . .
RUN npm run build

# Runtime stage
FROM nginx:alpine
COPY --from=build /app/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
