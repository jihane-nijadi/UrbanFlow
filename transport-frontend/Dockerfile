# Build stage
FROM node:18-alpine AS build
WORKDIR /app
COPY package*.json ./
# Ensure npm is available in the Alpine image and install build deps if missing
RUN apk add --no-cache nodejs npm
RUN npm ci
COPY . .
RUN npm run build

# Runtime stage
FROM nginx:alpine
COPY --from=build /app/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
